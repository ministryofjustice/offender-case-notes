package uk.gov.justice.hmpps.casenotes.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.config.RO_OPERATIONS
import uk.gov.justice.hmpps.casenotes.notes.ReadCaseNote
import uk.gov.justice.hmpps.casenotes.notes.SearchNotesRequest
import uk.gov.justice.hmpps.casenotes.notes.SearchNotesResponse

@Tag(name = RO_OPERATIONS)
@RestController
@RequestMapping("search/case-notes")
class SearchCaseNotesController(private val search: ReadCaseNote) {
  @Operation(
    summary = "Finds matching case notes",
    description = "Sorting can be applied on occurrenceDateTime (default) or creationDateTime. Any other sort parameter will have the default result (occurrenceDateTime,desc)",
  )
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK - successfully conducted search, providing matching results or empty content when no matching case notes are found"),
    ApiResponse(responseCode = "400", description = "Bad request - the search request did not meet validation requirements"),
  )
  @PostMapping("/{personIdentifier}")
  fun findCaseNotes(
    @Parameter(description = "Person Identifier", required = true, example = "A1234AA")
    @PathVariable personIdentifier: String,
    @Valid @RequestBody request: SearchNotesRequest,
  ): SearchNotesResponse = search.findNotes(personIdentifier.uppercase(), request)
}
