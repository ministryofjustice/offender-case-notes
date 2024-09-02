package uk.gov.justice.hmpps.casenotes.notes.internal

import jakarta.validation.Valid
import jakarta.validation.ValidationException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.dto.AmendCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.asLegacyId
import uk.gov.justice.hmpps.casenotes.services.EntityNotFoundException
import java.util.UUID.fromString

@Validated
@Service
@Transactional
@PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_WRITE')")
class WriteCaseNote(
  private val typeRepository: TypeRepository,
  private val noteRepository: NoteRepository,
) {
  fun note(prisonNumber: String, @Valid request: CreateCaseNoteRequest, useRestrictedType: Boolean): CaseNote {
    val type = typeRepository.findByCategoryCodeAndCode(request.type, request.subType)
      ?.validateRestrictedUse(useRestrictedType)
      ?: throw IllegalArgumentException("Unknown case note type ${request.type}/${request.subType}")

    if (!type.active) throw ValidationException("Case note type not active")

    return noteRepository.saveAndRefresh(request.toEntity(prisonNumber, type, CaseNoteRequestContext.get())).toModel()
  }

  fun amendment(
    prisonNumber: String,
    caseNoteId: String,
    @Valid request: AmendCaseNoteRequest,
    useRestrictedType: Boolean,
  ): CaseNote {
    val caseNote = when (val legacyId = caseNoteId.asLegacyId()) {
      null -> noteRepository.findByIdAndPrisonNumber(fromString(caseNoteId), prisonNumber)
      else -> noteRepository.findByLegacyIdAndPrisonNumber(legacyId, prisonNumber)
    }?.takeIf { it.prisonNumber == prisonNumber } ?: throw EntityNotFoundException.withId(caseNoteId)

    caseNote.type.validateRestrictedUse(useRestrictedType)

    return caseNote.addAmendment(request).toModel()
  }

  private fun Type.validateRestrictedUse(useRestrictedType: Boolean) = apply {
    if (restrictedUse && !useRestrictedType) {
      throw AccessDeniedException("Case note type is for restricted use, but useRestrictedType was not set")
    }
  }

  private fun CreateCaseNoteRequest.toEntity(
    prisonNumber: String,
    type: Type,
    context: CaseNoteRequestContext,
  ) = Note(
    prisonNumber,
    type,
    occurrenceDateTime,
    locationId!!,
    context.username,
    context.userId,
    context.userDisplayName,
    text,
    systemGenerated ?: false,
  )
}
