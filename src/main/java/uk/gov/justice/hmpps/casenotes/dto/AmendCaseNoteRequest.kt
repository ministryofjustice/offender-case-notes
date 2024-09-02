package uk.gov.justice.hmpps.casenotes.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

@Schema(description = "Amend a Case Note")
data class AmendCaseNoteRequest(
  @Schema(required = true, description = "Text of case note", example = "This is a case note message")
  @NotBlank
  @Length(max = 30000)
  val text: String,
)
