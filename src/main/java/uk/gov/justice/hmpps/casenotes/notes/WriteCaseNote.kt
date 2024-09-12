package uk.gov.justice.hmpps.casenotes.notes

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.getByParentCodeAndCode
import uk.gov.justice.hmpps.casenotes.domain.saveAndRefresh
import uk.gov.justice.hmpps.casenotes.services.EntityNotFoundException
import java.util.UUID.fromString

@Service
@Transactional
@PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_WRITE')")
class WriteCaseNote(
  private val subTypeRepository: SubTypeRepository,
  private val noteRepository: NoteRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun createNote(prisonNumber: String, request: CreateCaseNoteRequest, useRestrictedType: Boolean): CaseNote {
    val type = subTypeRepository.getByParentCodeAndCode(request.type, request.subType)
      .validateTypeUsage(useRestrictedType)

    if (!type.active) throw ValidationException("Case note type not active")

    return noteRepository.saveAndRefresh(request.toEntity(prisonNumber, type, CaseNoteRequestContext.get())).toModel()
  }

  fun createAmendment(
    prisonNumber: String,
    caseNoteId: String,
    request: AmendCaseNoteRequest,
    useRestrictedType: Boolean,
  ): CaseNote {
    val caseNote = getCaseNote(prisonNumber, caseNoteId).also {
      it.type.validateTypeUsage(useRestrictedType)
    }

    return noteRepository.saveAndFlush(caseNote.addAmendment(request)).toModel()
  }

  fun deleteNote(prisonNumber: String, caseNoteId: String) {
    getCaseNote(prisonNumber, caseNoteId).also(noteRepository::delete)
    telemetryClient.trackEvent(
      "CaseNoteSoftDelete",
      mapOf(
        "userName" to CaseNoteRequestContext.get().username,
        "prisonNumber" to prisonNumber,
        "caseNoteId" to caseNoteId,
      ),
      null,
    )
  }

  private fun getCaseNote(prisonNumber: String, caseNoteId: String): Note =
    when (val legacyId = caseNoteId.asLegacyId()) {
      null -> noteRepository.findByIdAndPrisonNumber(fromString(caseNoteId), prisonNumber)
      else -> noteRepository.findByLegacyIdAndPrisonNumber(legacyId, prisonNumber)
    }?.takeIf { it.prisonNumber == prisonNumber } ?: throw EntityNotFoundException.withId(caseNoteId)

  private fun SubType.validateTypeUsage(useRestrictedType: Boolean) = apply {
    if (restrictedUse && !useRestrictedType) {
      throw AccessDeniedException("Case note type is for restricted use, but useRestrictedType was not set")
    }
    if (!CaseNoteRequestContext.get().nomisUser && syncToNomis) {
      throw AccessDeniedException("Unable to author 'sync to nomis' type without a nomis user")
    }
  }

  private fun CreateCaseNoteRequest.toEntity(
    prisonNumber: String,
    type: SubType,
    context: CaseNoteRequestContext,
  ) = Note(
    prisonNumber,
    type,
    occurrenceDateTime ?: context.requestAt,
    locationId!!,
    context.username,
    context.userId,
    context.userDisplayName,
    text,
    systemGenerated ?: false,
  ).apply { legacyId = noteRepository.getNextLegacyId() }
}
