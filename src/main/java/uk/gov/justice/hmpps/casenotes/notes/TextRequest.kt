package uk.gov.justice.hmpps.casenotes.notes

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.constraints.NotBlank

interface TextRequest {
  @get:Schema(requiredMode = REQUIRED, description = "The text of the note")
  @get:NotBlank(message = "text cannot be blank")
  val text: String
}
