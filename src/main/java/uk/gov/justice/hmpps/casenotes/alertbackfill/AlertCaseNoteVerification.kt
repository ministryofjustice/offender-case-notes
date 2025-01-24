package uk.gov.justice.hmpps.casenotes.alertbackfill

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
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
import uk.gov.justice.hmpps.casenotes.integrations.ManageUsersService
import uk.gov.justice.hmpps.casenotes.integrations.UserDetails
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class AlertCaseNoteVerification(
  private val manageUsersService: ManageUsersService,
  private val alertService: AlertService,
  private val noteRepository: NoteRepository,
  private val subTypeRepository: SubTypeRepository,
  private val telemetryClient: TelemetryClient,
  private val serviceConfig: ServiceConfig,
) {
  fun verify(domainEvent: DomainEvent<AlertVerificationInformation>) {
    setDpsUserIfNotSet()
    val info = domainEvent.additionalInformation
    val alerts = alertService.getAlertsOfInterest(info.personIdentifier, info.from, info.to).content
    val existingNotes = noteRepository.findAll(info.asSpecification())
    val toCreate = alerts.flatMap {
      val matches = existingNotes matching it
      buildList {
        if (matches[ACTIVE] == null) add(ACTIVE to it)
        if (!it.isActive() && matches[INACTIVE] == null) add(INACTIVE to it)
      }
    }.groupBy({ it.first }, { it.second })

    if (toCreate.isEmpty()) return
    telemetryClient.trackEvent("MissingAlertCaseNotes", toCreate.properties(), mapOf())

    if (serviceConfig.actionMissingCaseNotes) {
      val subTypes = subTypeRepository.findByKeyIn(
        setOf(TypeKey("ALERT", "ACTIVE"), TypeKey("ALERT", "INACTIVE")),
      ).associateBy { ActiveInactive.valueOf(it.code) }
      val notes = toCreate.flatMap { e ->
        e.value.map { it.toNote(info.personIdentifier, e.key, subTypes) }
      }
      noteRepository.saveAll(notes)
    }
  }

  private fun AlertVerificationInformation.asSpecification() =
    matchesPersonIdentifier(personIdentifier)
      .and(matchesOnType(true, mapOf("ALERT" to setOf())))
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

  private fun Map<ActiveInactive, List<CaseNoteAlert>>.properties() = buildMap {
    this@properties[ACTIVE]?.also { v ->
      put("ActiveCount", v.size.toString())
      put("ActiveTypes", v.joinToString { "${it.type} ${it.subType}" })
    }
    this@properties[INACTIVE]?.also { v ->
      put("InactiveCount", v.size.toString())
      put("InactiveTypes", v.joinToString { "${it.type} ${it.subType}" })
    }
  }

  fun CaseNoteAlert.toNote(
    personIdentifier: String,
    activeInactive: ActiveInactive,
    subTypes: Map<ActiveInactive, SubType>,
  ) = Note(
    personIdentifier,
    requireNotNull(subTypes[activeInactive]),
    date(activeInactive),
    prisonCode!!,
    DPS_USERNAME,
    dpsUser?.userId ?: "2",
    dpsUser?.name ?: "Dps Synchronisation",
    when (activeInactive) {
      ACTIVE -> activeText()
      INACTIVE -> inactiveText()
    },
    true,
    System.DPS,
  ).apply {
    legacyId = noteRepository.getNextLegacyId()
    createdBy = DPS_USERNAME
    this.createdAt = date(activeInactive)
  }

  private fun CaseNoteAlert.date(activeInactive: ActiveInactive) = when (activeInactive) {
    ACTIVE -> createdAt.truncatedTo(ChronoUnit.DAYS)
    INACTIVE -> checkNotNull(activeTo).atStartOfDay()
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
  }
}

enum class ActiveInactive {
  ACTIVE,
  INACTIVE,
}

data class AlertVerificationInformation(val personIdentifier: String, val from: LocalDate, val to: LocalDate) :
  AdditionalInformation
