package uk.gov.justice.hmpps.casenotes.notes

import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_ADMIN
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.System
import uk.gov.justice.hmpps.casenotes.domain.getByTypeCodeAndCode
import uk.gov.justice.hmpps.casenotes.domain.saveAndRefresh
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Companion.createEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.CREATED
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.DELETED
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.UPDATED
import uk.gov.justice.hmpps.casenotes.legacy.service.EntityNotFoundException
import java.util.UUID.fromString

@Service
@Transactional
class WriteCaseNote(
  private val subTypeRepository: SubTypeRepository,
  private val noteRepository: NoteRepository,
  private val eventPublisher: ApplicationEventPublisher,
) {
  @PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_WRITE')")
  fun createNote(personIdentifier: String, request: CreateCaseNoteRequest): CaseNote {
    val type = subTypeRepository.getByTypeCodeAndCode(request.type, request.subType)
      .validateTypeUsage()

    val saved = noteRepository.saveAndRefresh(request.toEntity(personIdentifier, type, CaseNoteRequestContext.get()))
    eventPublisher.publishEvent(saved.createEvent(CREATED))
    return saved.toModel()
  }

  @PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_WRITE')")
  fun createAmendment(
    personIdentifier: String,
    caseNoteId: String,
    request: AmendCaseNoteRequest,
  ): CaseNote {
    val caseNote = getCaseNote(personIdentifier, caseNoteId).also {
      it.subType.validateTypeUsage()
    }

    caseNote.addAmendment(request)
    eventPublisher.publishEvent(caseNote.createEvent(UPDATED))
    return caseNote.toModel()
  }

  @PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_ADMIN')")
  fun deleteNote(personIdentifier: String, caseNoteId: String) {
    getCaseNote(personIdentifier, caseNoteId).also {
      noteRepository.delete(it)
      eventPublisher.publishEvent(it.createEvent(DELETED))
    }
  }

  private fun getCaseNote(personIdentifier: String, caseNoteId: String): Note = when (val legacyId = caseNoteId.asLegacyId()) {
    null -> noteRepository.findByIdAndPersonIdentifier(fromString(caseNoteId), personIdentifier)
    else -> noteRepository.findByLegacyIdAndPersonIdentifier(legacyId, personIdentifier)
  }?.takeIf { it.personIdentifier == personIdentifier } ?: throw EntityNotFoundException.withId(caseNoteId)

  private fun SubType.validateTypeUsage() = apply {
    if (!CaseNoteRequestContext.get().nomisUser && syncToNomis) {
      throw AccessDeniedException("Unable to author 'sync to nomis' type without a nomis user")
    }
  }

  private fun CreateCaseNoteRequest.toEntity(
    personIdentifier: String,
    type: SubType,
    context: CaseNoteRequestContext,
  ) = Note(
    personIdentifier,
    type,
    occurrenceDateTime ?: context.requestAt,
    locationId!!,
    context.username,
    context.userId,
    context.userDisplayName,
    text,
    systemGenerated ?: false,
    System.DPS,
  ).apply { legacyId = noteRepository.getNextLegacyId() }
}
