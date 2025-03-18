package uk.gov.justice.hmpps.casenotes.alertnotes

import org.springframework.stereotype.Component
import uk.gov.justice.hmpps.casenotes.alertnotes.ActiveInactive.ACTIVE
import uk.gov.justice.hmpps.casenotes.alertnotes.ActiveInactive.INACTIVE
import uk.gov.justice.hmpps.casenotes.alertnotes.AlertCaseNoteReconciliation.Companion.TYPE
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.System
import uk.gov.justice.hmpps.casenotes.domain.TypeKey
import uk.gov.justice.hmpps.casenotes.events.AdditionalInformation
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.integrations.ManageUsersService
import uk.gov.justice.hmpps.casenotes.integrations.PrisonApiService
import uk.gov.justice.hmpps.casenotes.integrations.PrisonerSearchService
import uk.gov.justice.hmpps.casenotes.integrations.UserDetails
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

@Component
class AlertCaseNoteHandler(
  private val alertsService: AlertService,
  private val prisonerSearchService: PrisonerSearchService,
  private val prisonApiService: PrisonApiService,
  private val manageUsersService: ManageUsersService,
  private val subTypeRepository: SubTypeRepository,
  private val noteRepository: NoteRepository,
) {
  fun handleAlertCreated(domainEvent: DomainEvent<AlertAdditionalInformation>) {
    val alert = alertsService.getAlert(domainEvent.additionalInformation.alertUuid)
    val prisonCode = prisonerSearchService.getPrisonerDetails(alert.prisonNumber).prisonId
    if (!prisonApiService.alertCaseNotesFor(prisonCode)) return

    val userDetail = manageUsersService.getUserDetails(alert.createdBy)
    val subType = checkNotNull(subTypeRepository.findByKey(TypeKey(TYPE, ACTIVE.name)))
    noteRepository.save(alert.toNote(prisonCode, ACTIVE, subType, userDetail, null))
  }

  fun handleAlertInactive(domainEvent: DomainEvent<AlertAdditionalInformation>) {
    val alert = alertsService.getAlert(domainEvent.additionalInformation.alertUuid)
    val prisonCode = prisonerSearchService.getPrisonerDetails(alert.prisonNumber).prisonId
    if (!prisonApiService.alertCaseNotesFor(prisonCode)) return

    val username = alert.inactiveUsername()
    val userDetail = username?.let { manageUsersService.getUserDetails(it) }
    val subType = checkNotNull(subTypeRepository.findByKey(TypeKey(TYPE, INACTIVE.name)))
    noteRepository.save(alert.toNote(prisonCode, INACTIVE, subType, userDetail, domainEvent.occurredAt.toLocalDateTime()))
  }

  data class AlertAdditionalInformation(
    val alertUuid: UUID,
  ) : AdditionalInformation

  fun Alert.toNote(
    prisonCode: String,
    activeInactive: ActiveInactive,
    subType: SubType,
    userDetails: UserDetails?,
    madeInactiveAt: LocalDateTime?,
  ) = Note(
    prisonNumber,
    subType,
    when (activeInactive) {
      ACTIVE -> activeFrom.atStartOfDay()
      INACTIVE -> requireNotNull(activeTo).atStartOfDay()
    },
    prisonCode,
    userDetails?.username ?: "OMS_OWNER",
    userDetails?.userId ?: "1",
    userDetails?.name ?: "System Generated",
    when (activeInactive) {
      ACTIVE -> activeText()
      INACTIVE -> inactiveText()
    },
    true,
    System.DPS,
  ).apply {
    legacyId = noteRepository.getNextLegacyId()
    createdBy = userDetails?.username ?: "OMS_OWNER"
    this.createdAt = when (activeInactive) {
      ACTIVE -> this@toNote.createdAt.truncatedTo(SECONDS)
      INACTIVE -> checkNotNull(madeInactiveAt)
    }
  }
}
