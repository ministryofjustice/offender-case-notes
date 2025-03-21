package uk.gov.justice.hmpps.casenotes.alertnotes

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.alertnotes.ActiveInactive.ACTIVE
import uk.gov.justice.hmpps.casenotes.alertnotes.ActiveInactive.INACTIVE
import uk.gov.justice.hmpps.casenotes.alertnotes.AlertCaseNoteReconciliation.Companion.TYPE
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.System
import uk.gov.justice.hmpps.casenotes.domain.TypeKey
import uk.gov.justice.hmpps.casenotes.events.AdditionalInformation
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Companion.createEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.CREATED
import uk.gov.justice.hmpps.casenotes.integrations.ManageUsersService
import uk.gov.justice.hmpps.casenotes.integrations.PrisonApiService
import uk.gov.justice.hmpps.casenotes.integrations.PrisonerSearchService
import uk.gov.justice.hmpps.casenotes.integrations.UserDetails
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

@Transactional
@Component
class AlertCaseNoteHandler(
  private val alertsService: AlertService,
  private val prisonerSearchService: PrisonerSearchService,
  private val prisonApiService: PrisonApiService,
  private val manageUsersService: ManageUsersService,
  private val subTypeRepository: SubTypeRepository,
  private val noteRepository: NoteRepository,
  private val eventPublisher: ApplicationEventPublisher,
) {
  fun handleAlertCreated(domainEvent: DomainEvent<AlertAdditionalInformation>) {
    val (alert, prisonCode) = domainEvent.alertAndLocation()
    if (!prisonApiService.alertCaseNotesFor(prisonCode)) return

    val userDetail = manageUsersService.getUserDetails(alert.createdBy)
    val saved = noteRepository.save(alert.toNote(prisonCode, userDetail, alert.createdAt))
    eventPublisher.publishEvent(saved.createEvent(CREATED, sourceOverride = Source.DPS))
  }

  fun handleAlertInactive(domainEvent: DomainEvent<AlertAdditionalInformation>) {
    val (alert, prisonCode) = domainEvent.alertAndLocation()
    if (!prisonApiService.alertCaseNotesFor(prisonCode)) return

    val username = alert.inactiveUsername()
    val userDetail = username?.let { manageUsersService.getUserDetails(it) }
    val saved = noteRepository.save(alert.toNote(prisonCode, userDetail, domainEvent.occurredAt.toLocalDateTime()))
    eventPublisher.publishEvent(saved.createEvent(CREATED, sourceOverride = Source.DPS))
  }

  private fun DomainEvent<AlertAdditionalInformation>.alertAndLocation(): Pair<Alert, String> {
    val alert = alertsService.getAlert(additionalInformation.alertUuid)
    val prisonCode = prisonerSearchService.getPrisonerDetails(alert.prisonNumber).prisonId
    return alert to prisonCode
  }

  data class AlertAdditionalInformation(
    val alertUuid: UUID,
  ) : AdditionalInformation

  fun Alert.activeInactive() = if (isActive) ACTIVE else INACTIVE

  fun Alert.toNote(
    prisonCode: String,
    userDetails: UserDetails?,
    madeInactiveAt: LocalDateTime?,
  ) = Note(
    prisonNumber,
    checkNotNull(subTypeRepository.findByKey(TypeKey(TYPE, activeInactive().name))),
    when (activeInactive()) {
      ACTIVE -> activeFrom.atStartOfDay()
      INACTIVE -> requireNotNull(activeTo).atStartOfDay()
    },
    prisonCode,
    userDetails?.username ?: "OMS_OWNER",
    userDetails?.userId ?: "1",
    userDetails?.name ?: "System Generated",
    when (activeInactive()) {
      ACTIVE -> activeText()
      INACTIVE -> inactiveText()
    },
    true,
    System.DPS,
  ).apply {
    legacyId = noteRepository.getNextLegacyId()
    createdBy = userDetails?.username ?: "OMS_OWNER"
    this.createdAt = when (activeInactive()) {
      ACTIVE -> this@toNote.createdAt.truncatedTo(SECONDS)
      INACTIVE -> checkNotNull(madeInactiveAt)
    }
  }
}
