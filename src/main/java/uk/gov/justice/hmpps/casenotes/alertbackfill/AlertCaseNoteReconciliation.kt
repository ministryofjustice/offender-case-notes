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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    val existingNotes = noteRepository.findAll(info.asSpecification(from, to))
    val toCreate = alerts.flatMap {
      val matches = existingNotes matching it
      buildList {
        if (matches[ACTIVE] == null) add(ACTIVE to it)
        if (it.madeInactive() && matches[INACTIVE] == null) add(INACTIVE to it)
      }
    }.groupBy({ it.first }, { it.second })

    if (toCreate.isEmpty()) return
    telemetryClient.trackEvent("MissingAlertCaseNotes", toCreate.properties(info.personIdentifier), mapOf())

    if (serviceConfig.actionMissingCaseNotes) {
      val subTypes = subTypeRepository.findByKeyIn(
        setOf(TypeKey(TYPE, ACTIVE.name), TypeKey(TYPE, INACTIVE.name)),
      ).associateBy { ActiveInactive.valueOf(it.code) }
      val notes = toCreate.flatMap { e ->
        e.value.map { it.toNote(info.personIdentifier, e.key, subTypes) }
      }
      noteRepository.saveAll(notes).forEach { eventPublisher.publishEvent(it.createEvent(CREATED)) }
    }
  }

  private fun AlertReconciliationInformation.asSpecification(from: LocalDate, to: LocalDate) =
    matchesPersonIdentifier(personIdentifier)
      .and(matchesOnType(true, mapOf(TYPE to setOf())))
      .and(createdBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), true))

  private infix fun List<Note>.matching(alert: CaseNoteAlert): Map<ActiveInactive, Note> = mapNotNull { note ->
    when {
      note.matches(alert.activeText(), alert.activeFrom) -> ACTIVE to note
      alert.activeTo != null && note.matches(alert.inactiveText(), alert.activeTo) -> INACTIVE to note
      else -> null
    }
  }.toMap()

  private fun Note.matches(text: String, date: LocalDate) =
    this.text == text && occurredAt.toLocalDate().equals(date)

  private fun Map<ActiveInactive, List<CaseNoteAlert>>.properties(personIdentifier: String) = buildMap {
    put("PersonIdentifier", personIdentifier)
    this@properties[ACTIVE]?.also { v ->
      put("ActiveCount", v.size.toString())
      put(
        "ActiveTypes",
        v.joinToString { "${it.type.description} and ${it.subType.description} -> ${it.activeFrom} | ${it.createdAt}" },
      )
    }
    this@properties[INACTIVE]?.also { v ->
      put("InactiveCount", v.size.toString())
      put(
        "InactiveTypes",
        v.joinToString { "${it.type.description} and ${it.subType.description} -> ${it.activeTo} | ${it.madeInactiveAt}" },
      )
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
      ACTIVE -> this@toNote.createdAt.truncatedTo(ChronoUnit.SECONDS)
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

data class AlertReconciliationInformation(val personIdentifier: String, val from: LocalDate, val to: LocalDate) :
  AdditionalInformation

private fun List<CaseNoteAlert>.getCaseNoteDates(): Pair<LocalDate, LocalDate> {
  val dates = flatMap {
    listOfNotNull(it.createdAt.toLocalDate(), it.madeInactiveAt?.toLocalDate(), it.activeTo, it.activeFrom)
  }.sorted()
  return Pair(dates.first(), dates.last())
}
