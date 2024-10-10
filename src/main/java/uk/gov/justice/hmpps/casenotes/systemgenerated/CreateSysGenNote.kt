package uk.gov.justice.hmpps.casenotes.systemgenerated

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext.Companion.get
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.System
import uk.gov.justice.hmpps.casenotes.domain.getByTypeCodeAndCode
import uk.gov.justice.hmpps.casenotes.domain.saveAndRefresh
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Companion.createEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.CREATED
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.toModel

@Transactional
@Service
class CreateSysGenNote(
  private val subTypeRepository: SubTypeRepository,
  private val noteRepository: NoteRepository,
  private val eventPublisher: ApplicationEventPublisher,
) {
  fun systemGeneratedCaseNote(personIdentifier: String, request: SystemGeneratedRequest): CaseNote {
    val type = subTypeRepository.getByTypeCodeAndCode(request.type, request.subType).validated()
    val saved = noteRepository.saveAndRefresh(request.toEntity(personIdentifier, type, get()))
    eventPublisher.publishEvent(saved.createEvent(CREATED))
    return saved.toModel()
  }

  private fun SubType.validated() = apply {
    check(!syncToNomis) { "System generated case notes cannot use a sync to nomis type" }
  }

  private fun SystemGeneratedRequest.toEntity(
    prisonNumber: String,
    type: SubType,
    context: CaseNoteRequestContext,
  ) = Note(
    prisonNumber,
    type,
    occurrenceDateTime ?: context.requestAt,
    requireNotNull(locationId),
    authorUsername ?: context.username,
    context.userId,
    authorName,
    text,
    true,
    System.DPS,
  ).apply { legacyId = noteRepository.getNextLegacyId() }
}
