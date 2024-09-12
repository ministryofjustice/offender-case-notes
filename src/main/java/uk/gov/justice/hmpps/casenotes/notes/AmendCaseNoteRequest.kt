package uk.gov.justice.hmpps.casenotes.notes

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Amend a Case Note")
data class AmendCaseNoteRequest(
  @Schema(required = true, description = "Text to be appended to the case note")
  override val text: String,
) : TextRequest
