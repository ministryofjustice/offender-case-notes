package uk.gov.justice.hmpps.casenotes.notes

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.Valid
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.domain.AmendmentRepository
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.saveAndRefresh
import uk.gov.justice.hmpps.casenotes.services.EntityNotFoundException
import java.util.UUID.fromString

@Validated
@Service
@Transactional
@PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_WRITE')")
class WriteCaseNote(
  private val subTypeRepository: SubTypeRepository,
  private val noteRepository: NoteRepository,
  private val amendmentRepository: AmendmentRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun createNote(prisonNumber: String, @Valid request: CreateCaseNoteRequest, useRestrictedType: Boolean): CaseNote {
    val type = subTypeRepository.findByParentCodeAndCode(request.type, request.subType)
      ?.validateRestrictedUse(useRestrictedType)
      ?: throw IllegalArgumentException("Unknown case note type ${request.type}/${request.subType}")

    if (!type.active) throw ValidationException("Case note type not active")

    return noteRepository.saveAndRefresh(request.toEntity(prisonNumber, type, CaseNoteRequestContext.get())).toModel()
  }

  fun createAmendment(
    prisonNumber: String,
    caseNoteId: String,
    @Valid request: AmendCaseNoteRequest,
    useRestrictedType: Boolean,
  ): CaseNote {
    val caseNote = getCaseNote(prisonNumber, caseNoteId).also {
      it.type.validateRestrictedUse(useRestrictedType)
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

  fun deleteAmendment(prisonNumber: String, amendmentId: Long) {
    amendmentRepository.findByIdOrNull(amendmentId)?.also {
      amendmentRepository.delete(it)
      telemetryClient.trackEvent(
        "CaseNoteAmendmentSoftDelete",
        mapOf(
          "userName" to CaseNoteRequestContext.get().username,
          "prisonNumber" to prisonNumber,
          "caseNoteId" to it.note.id.toString(),
          "amendmentId" to amendmentId.toString(),
        ),
        null,
      )
    }
  }

  private fun getCaseNote(prisonNumber: String, caseNoteId: String): Note =
    when (val legacyId = caseNoteId.asLegacyId()) {
      null -> noteRepository.findByIdAndPrisonNumber(fromString(caseNoteId), prisonNumber)
      else -> noteRepository.findByLegacyIdAndPrisonNumber(legacyId, prisonNumber)
    }?.takeIf { it.prisonNumber == prisonNumber } ?: throw EntityNotFoundException.withId(caseNoteId)

  private fun SubType.validateRestrictedUse(useRestrictedType: Boolean) = apply {
    if (restrictedUse && !useRestrictedType) {
      throw AccessDeniedException("Case note type is for restricted use, but useRestrictedType was not set")
    }
  }

  private fun CreateCaseNoteRequest.toEntity(
    prisonNumber: String,
    type: SubType,
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
  ).apply { new = true }
}
