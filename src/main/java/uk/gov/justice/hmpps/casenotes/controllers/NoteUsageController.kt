package uk.gov.justice.hmpps.casenotes.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.legacy.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.notes.NoteUsageResponse
import uk.gov.justice.hmpps.casenotes.notes.ReadCaseNote
import uk.gov.justice.hmpps.casenotes.notes.UsageByAuthorIdRequest
import uk.gov.justice.hmpps.casenotes.notes.UsageByAuthorIdResponse
import uk.gov.justice.hmpps.casenotes.notes.UsageByPersonIdentifierRequest
import uk.gov.justice.hmpps.casenotes.notes.UsageByPersonIdentifierResponse

@Tag(name = "case-note-usage", description = "Case Note Usage")
@RestController
@RequestMapping("case-notes")
class NoteUsageController(private val usage: ReadCaseNote) {
  @Operation(summary = "Finds counts of case notes for person identifier")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK - counts returned based on request"),
    ApiResponse(
      responseCode = "400",
      description = "Bad Request - Request validation failed",
      content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "401",
      description = "Unauthorised, requires a valid token",
      content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "403",
      description = "Forbidden, requires an appropriate role",
      content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  @PostMapping("/usage")
  fun noteUsageForPersonIdentifier(
    @Valid @RequestBody request: UsageByPersonIdentifierRequest,
  ): NoteUsageResponse<UsageByPersonIdentifierResponse> = NoteUsageResponse(usage.findByPersonIdentifier(request))

  @Operation(summary = "Finds counts of case notes for an author")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK - counts returned based on request"),
    ApiResponse(
      responseCode = "400",
      description = "Bad Request - Request validation failed",
      content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "401",
      description = "Unauthorised, requires a valid token",
      content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "403",
      description = "Forbidden, requires an appropriate role",
      content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  @PostMapping("/staff-usage")
  fun noteUsageForAuthorUserId(
    @Valid @RequestBody request: UsageByAuthorIdRequest,
  ): NoteUsageResponse<UsageByAuthorIdResponse> = NoteUsageResponse(usage.findByAuthorId(request))
}
