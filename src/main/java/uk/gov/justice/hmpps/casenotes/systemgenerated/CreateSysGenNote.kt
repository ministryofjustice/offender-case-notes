package uk.gov.justice.hmpps.casenotes.systemgenerated

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext.Companion.get
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.findByParentCodeAndCode
import uk.gov.justice.hmpps.casenotes.domain.saveAndRefresh
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.toModel

@Transactional
@Service
class CreateSysGenNote(
  private val subTypeRepository: SubTypeRepository,
  private val noteRepository: NoteRepository,
) {
  fun systemGeneratedCaseNote(personIdentifier: String, request: SystemGeneratedRequest): CaseNote {
    val type = subTypeRepository.findByParentCodeAndCode(request.type, request.subType)?.validated()
      ?: throw IllegalArgumentException("Unknown case note type ${request.type}:${request.subType}")
    return noteRepository.saveAndRefresh(request.toEntity(personIdentifier, type, get())).toModel()
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
    locationId,
    authorUsername ?: context.username,
    context.userId,
    authorName,
    text,
    true,
  ).apply { legacyId = noteRepository.getNextLegacyId() }
}
