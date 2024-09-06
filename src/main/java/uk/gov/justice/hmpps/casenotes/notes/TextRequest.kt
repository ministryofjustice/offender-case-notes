package uk.gov.justice.hmpps.casenotes.notes

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

interface TextRequest {
  val text: String
}

@Schema(description = "Amend a Case Note")
data class AmendCaseNoteRequest(
  @Schema(required = true, description = "Text of case note", example = "This is a case note message")
  @NotBlank
  @Length(max = 30000)
  override val text: String,
) : TextRequest
