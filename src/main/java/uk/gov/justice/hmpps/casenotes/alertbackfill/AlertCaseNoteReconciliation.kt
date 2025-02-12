package uk.gov.justice.hmpps.casenotes.alertbackfill

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.annotation.PostConstruct
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.alertbackfill.ActiveInactive.ACTIVE
import uk.gov.justice.hmpps.casenotes.alertbackfill.ActiveInactive.INACTIVE
import uk.gov.justice.hmpps.casenotes.config.ServiceConfig
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.System
import uk.gov.justice.hmpps.casenotes.domain.TypeKey
import uk.gov.justice.hmpps.casenotes.domain.createdBetween
import uk.gov.justice.hmpps.casenotes.domain.matchesOnType
import uk.gov.justice.hmpps.casenotes.domain.matchesPersonIdentifier
import uk.gov.justice.hmpps.casenotes.events.AdditionalInformation
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Companion.createEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.CREATED
import uk.gov.justice.hmpps.casenotes.integrations.ManageUsersService
import uk.gov.justice.hmpps.casenotes.integrations.UserDetails
import java.time.Duration.between
import java.time.Duration.ofMinutes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.SECONDS

@Transactional
@Service
class AlertCaseNoteReconciliation(
  private val manageUsersService: ManageUsersService,
  private val alertService: AlertService,
  private val noteRepository: NoteRepository,
  private val subTypeRepository: SubTypeRepository,
  private val telemetryClient: TelemetryClient,
  private val eventPublisher: ApplicationEventPublisher,
  private val serviceConfig: ServiceConfig,
) {
  fun reconcile(domainEvent: DomainEvent<AlertReconciliationInformation>) {
    setDpsUserIfNotSet()
    val info = domainEvent.additionalInformation
    val alerts = alertService.getAlertsOfInterest(info.personIdentifier, info.from, info.to).content
    if (alerts.isEmpty()) return
    val (from, to) = alerts.getCaseNoteDates()
    val existingNotes = noteRepository.findAll(info.asSpecification(from, maxOf(to, info.to)))
    val toCreate = alerts.flatMap {
      val matches = existingNotes matching it
      matches.findMissing(it, info)
    }.groupBy({ it.status to it.scope }, { it.alert })

    if (toCreate.isEmpty()) return
    telemetryClient.trackEvent("MissingAlertCaseNotes", toCreate.properties(info.personIdentifier), mapOf())

    if (serviceConfig.actionMissingCaseNotes) {
      val subTypes = subTypeRepository.findByKeyIn(
        setOf(TypeKey(TYPE, ACTIVE.name), TypeKey(TYPE, INACTIVE.name)),
      ).associateBy { ActiveInactive.valueOf(it.code) }
      val notes = toCreate.flatMap { e ->
        e.value.map { it.toNote(info.personIdentifier, e.key.first, subTypes) }
      }
      noteRepository.saveAll(notes).forEach { eventPublisher.publishEvent(it.createEvent(CREATED)) }
    }
  }

  private fun Set<ActiveInactive>.findMissing(
    alert: CaseNoteAlert,
    info: AlertReconciliationInformation,
  ): Set<MissingNote> = buildSet {
    if (!contains<ActiveInactive>(ACTIVE)) {
      add(
        MissingNote(
          ACTIVE,
          if (alert.activeFrom in (info.from..info.to)) InOutScope.IN else InOutScope.OUT,
          alert,
        ),
      )
      if (alert.isActive()) {
        telemetryClient.trackEvent(
          "MissingActiveAlertCaseNote",
          listOfNotNull(
            "personIdentifier" to info.personIdentifier,
            "type" to alert.type.description,
            "subType" to alert.subType.description,
            "from" to DateTimeFormatter.ISO_DATE.format(alert.activeFrom),
            alert.activeTo?.let { "to" to DateTimeFormatter.ISO_DATE.format(it) },
            "createdAt" to DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(alert.createdAt),
            alert.madeInactiveAt?.let { "madeInactiveAt" to DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(it) },
          ).toMap(),
          mapOf(),
        )
      }
    }
    if (alert.madeInactive() && !contains<ActiveInactive>(INACTIVE)) {
      add(
        MissingNote(
          INACTIVE,
          if (alert.activeTo!! in (info.from..info.to)) InOutScope.IN else InOutScope.OUT,
          alert,
        ),
      )
    }
  }

  private fun AlertReconciliationInformation.asSpecification(from: LocalDate, to: LocalDate) = matchesPersonIdentifier(personIdentifier)
    .and(matchesOnType(true, mapOf(TYPE to setOf())))
    .and(createdBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), true))

  private infix fun List<Note>.matching(alert: CaseNoteAlert): Set<ActiveInactive> = flatMap { note ->
    buildSet {
      if (note.matchesActive(alert)) {
        add(ACTIVE)
      }
      if (alert.activeTo != null && note.matchesInactive(alert)) {
        add(INACTIVE)
      }
    }
  }.toSet()

  private fun Note.matchesActive(alert: CaseNoteAlert) = (this.text == alert.activeText() || this.text == alert.alternativeActiveText()) &&
    (
      this.occurredAt.toLocalDate() == alert.activeFrom ||
        between(alert.createdAt.truncatedTo(SECONDS), this.createdAt.truncatedTo(SECONDS)).abs() <= ofMinutes(1)
      )

  private fun Note.matchesInactive(alert: CaseNoteAlert) = (this.text == alert.inactiveText() || this.text == alert.alternativeInactiveText()) &&
    this.occurredAt.toLocalDate() == alert.activeTo

  private fun Map<Pair<ActiveInactive, InOutScope>, List<CaseNoteAlert>>.properties(personIdentifier: String): Map<String, String> {
    val activeDescription: (CaseNoteAlert) -> String = {
      "${it.type.description} and ${it.subType.description} -> ${it.activeFrom} | ${it.createdAt}"
    }
    val inactiveDescription: (CaseNoteAlert) -> String = {
      "${it.type.description} and ${it.subType.description} -> ${it.activeTo} | ${it.madeInactiveAt}"
    }
    return buildMap {
      put("PersonIdentifier", personIdentifier)
      this@properties[ACTIVE to InOutScope.IN]?.also { v ->
        put("ActiveInScopeCount", v.size.toString())
        put(
          "ActiveInScopeTypes",
          v.joinToString { activeDescription(it) },
        )
      }
      this@properties[ACTIVE to InOutScope.OUT]?.also { v ->
        put("ActiveOutOfScopeCount", v.size.toString())
        put(
          "ActiveOutOfScopeTypes",
          v.joinToString { activeDescription(it) },
        )
      }
      this@properties[INACTIVE to InOutScope.IN]?.also { v ->
        put("InactiveInScopeCount", v.size.toString())
        put(
          "InactiveInScopeTypes",
          v.joinToString { inactiveDescription(it) },
        )
      }
      this@properties[INACTIVE to InOutScope.OUT]?.also { v ->
        put("InactiveOutOfScopeCount", v.size.toString())
        put(
          "InactiveOutOfScopeTypes",
          v.joinToString { inactiveDescription(it) },
        )
      }
    }
  }

  fun CaseNoteAlert.toNote(
    personIdentifier: String,
    activeInactive: ActiveInactive,
    subTypes: Map<ActiveInactive, SubType>,
  ) = Note(
    personIdentifier,
    requireNotNull(subTypes[activeInactive]),
    when (activeInactive) {
      ACTIVE -> activeFrom.atStartOfDay()
      INACTIVE -> requireNotNull(activeTo).atStartOfDay()
    },
    prisonCode!!,
    DPS_USERNAME,
    dpsUser?.userId ?: "2",
    dpsUser?.name ?: "System Generated",
    when (activeInactive) {
      ACTIVE -> activeText()
      INACTIVE -> inactiveText()
    },
    true,
    System.DPS,
  ).apply {
    legacyId = noteRepository.getNextLegacyId()
    createdBy = DPS_USERNAME
    this.createdAt = when (activeInactive) {
      ACTIVE -> this@toNote.createdAt.truncatedTo(SECONDS)
      INACTIVE -> checkNotNull(madeInactiveAt)
    }
  }

  @PostConstruct
  fun init() {
    setDpsUserIfNotSet()
  }

  private fun setDpsUserIfNotSet() {
    if (dpsUser == null) {
      dpsUser = try {
        manageUsersService.getUserDetails(DPS_USERNAME)
      } catch (_: Exception) {
        null
      }
    }
  }

  companion object {
    const val DPS_USERNAME: String = "PRISONER_MANAGER_API"
    private var dpsUser: UserDetails? = null
    const val TYPE = "ALERT"
  }
}

enum class ActiveInactive {
  ACTIVE,
  INACTIVE,
}

enum class InOutScope {
  IN,
  OUT,
}

data class MissingNote(val status: ActiveInactive, val scope: InOutScope, val alert: CaseNoteAlert)

data class AlertReconciliationInformation(val personIdentifier: String, val from: LocalDate, val to: LocalDate) : AdditionalInformation

private fun List<CaseNoteAlert>.getCaseNoteDates(): Pair<LocalDate, LocalDate> {
  val dates = flatMap {
    listOfNotNull(it.createdAt.toLocalDate(), it.madeInactiveAt?.toLocalDate(), it.activeTo, it.activeFrom)
  }.sorted()
  return Pair(dates.first(), dates.last())
}
