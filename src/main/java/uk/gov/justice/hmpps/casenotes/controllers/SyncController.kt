package uk.gov.justice.hmpps.casenotes.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_SYNC
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.sync.SyncCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncCaseNotes
import uk.gov.justice.hmpps.casenotes.sync.SyncResult

@Tag(name = "Sync Case Notes", description = "Endpoint for sync operations")
@RestController
@RequestMapping("sync/case-notes")
class SyncController(private val sync: SyncCaseNotes) {
  @Operation(
    summary = "Endpoint to migrate and/or sync case notes from nomis to dps.",
    description = "Case notes that don't exist in dps will be created, those that already exist and can be identified will be updated. Conceptually, a merge endpoint."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Case Notes successfully merged",
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
    ],
  )
  @PutMapping
  @PreAuthorize("hasRole('$ROLE_CASE_NOTES_SYNC')")
  fun syncCaseNotes(@Valid @RequestBody caseNotes: List<SyncCaseNoteRequest>): List<SyncResult> = sync.caseNotes(caseNotes)
}
