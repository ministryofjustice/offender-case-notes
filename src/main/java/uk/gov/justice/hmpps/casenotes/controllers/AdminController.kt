package uk.gov.justice.hmpps.casenotes.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.config.ADMIN_ONLY
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_ADMIN
import uk.gov.justice.hmpps.casenotes.legacy.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteAdminService
import uk.gov.justice.hmpps.casenotes.notes.DeleteCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.ReplaceNoteRequest
import java.util.UUID

@Tag(name = ADMIN_ONLY)
@RestController
@RequestMapping("admin")
@PreAuthorize("hasRole('$ROLE_CASE_NOTES_ADMIN')")
class AdminController(private val caseNoteAdmin: CaseNoteAdminService) {

  @Operation(summary = "Endpoint to replace a case note and amendments.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Case Note successfully replaced",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Case note not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/case-notes/{id}")
  fun replaceCaseNote(
    @PathVariable id: UUID,
    @Valid @RequestBody request: ReplaceNoteRequest,
  ): CaseNote {
    CaseNoteRequestContext.get().deletionReason = request.reason
    return caseNoteAdmin.replaceCaseNote(id, request)
  }

  @Operation(summary = "Endpoint to delete an existing case note")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Case Note successfully deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/case-notes/{id}")
  fun deleteCaseNote(
    @PathVariable id: UUID,
    @RequestBody request: DeleteCaseNoteRequest,
  ) {
    CaseNoteRequestContext.get().deletionReason = request.reason
    caseNoteAdmin.deleteNote(id)
  }
}
