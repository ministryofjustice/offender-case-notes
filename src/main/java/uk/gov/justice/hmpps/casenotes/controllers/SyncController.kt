package uk.gov.justice.hmpps.casenotes.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_SYNC
import uk.gov.justice.hmpps.casenotes.legacy.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.sync.MigrateCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.sync.MigrationResult
import uk.gov.justice.hmpps.casenotes.sync.MoveCaseNotesRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncCaseNotes
import uk.gov.justice.hmpps.casenotes.sync.SyncResult
import java.util.UUID

@Tag(name = "Sync Case Notes", description = "Endpoint for sync operations")
@RestController
class SyncController(private val sync: SyncCaseNotes) {
  @Operation(summary = "Endpoint repurposed to remove duplicate case notes")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Case Notes successfully migrated",
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
  @PostMapping("migrate/case-notes/{personIdentifier}")
  @PreAuthorize("hasRole('$ROLE_CASE_NOTES_SYNC')")
  fun migrateCaseNotes(
    @Parameter(description = "Person Identifier", required = true, example = "A1234AA")
    @PathVariable personIdentifier: String,
    @Valid @RequestBody caseNotes: List<MigrateCaseNoteRequest>,
  ): List<MigrationResult> = sync.removeUnknownNotes(personIdentifier, caseNotes)

  @Operation(
    summary = "Endpoint to sync a case note from nomis to dps.",
    description = "Case notes that don't exist in dps will be created, those that already exist and can be identified will be updated. Conceptually, a merge endpoint.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Case Note successfully created",
      ),
      ApiResponse(
        responseCode = "200",
        description = "Case Note successfully updated",
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
  @PutMapping("sync/case-notes")
  @PreAuthorize("hasRole('$ROLE_CASE_NOTES_SYNC')")
  fun syncCaseNotes(@Valid @RequestBody request: SyncCaseNoteRequest): ResponseEntity<SyncResult> =
    sync.syncNote(request).let {
      when (it.action) {
        SyncResult.Action.CREATED -> ResponseEntity.status(CREATED).body(it)
        SyncResult.Action.UPDATED -> ResponseEntity.ok(it)
      }
    }

  @Operation(
    summary = "Endpoint to delete a case note - only for sync operations",
    description = "Case notes that exist will be deleted. No exception will be returned if the case note does not exist.",
  )
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
  @DeleteMapping("sync/case-notes/{id}")
  @PreAuthorize("hasRole('$ROLE_CASE_NOTES_SYNC')")
  fun deleteCaseNote(@PathVariable id: UUID) = sync.deleteCaseNote(id)

  @GetMapping("sync/case-notes/{personIdentifier}")
  @PreAuthorize("hasRole('$ROLE_CASE_NOTES_SYNC')")
  fun getNomisCaseNotes(@PathVariable personIdentifier: String) = sync.getCaseNotes(personIdentifier)

  @Operation(
    summary = "Endpoint to move case notes - only for sync operations",
    description = "Case notes that are moved across bookings in nomis can be moved using this endpoint",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Case notes successfully moved",
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
  @PutMapping("/move/case-notes")
  @PreAuthorize("hasRole('$ROLE_CASE_NOTES_SYNC')")
  fun moveCaseNotes(@RequestBody request: MoveCaseNotesRequest) {
    sync.moveCaseNotes(request)
  }
}
