package uk.gov.justice.hmpps.casenotes.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.notes.NoteUsageResponse
import uk.gov.justice.hmpps.casenotes.notes.ReadCaseNote
import uk.gov.justice.hmpps.casenotes.notes.UsageByPersonIdentifierRequest
import uk.gov.justice.hmpps.casenotes.notes.UsageByPersonIdentifierResponse

@Tag(name = "case-note-usage", description = "Case Note Usage")
@RestController
@RequestMapping("case-notes")
class NoteUsageController(private val usage: ReadCaseNote) {
  @Operation(summary = "Finds counts of case notes for person identifier")
  @ApiResponses(ApiResponse(responseCode = "200", description = "OK - counts returned based on request"))
  @PostMapping("/usage")
  fun noteUsageForPersonIdentifier(
    @Valid @RequestBody request: UsageByPersonIdentifierRequest,
  ): NoteUsageResponse<UsageByPersonIdentifierResponse> = NoteUsageResponse(usage.findByPersonIdentifier(request))
}
