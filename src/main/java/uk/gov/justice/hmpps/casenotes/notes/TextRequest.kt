package uk.gov.justice.hmpps.casenotes.notes

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED

interface TextRequest {
  @get:Schema(requiredMode = REQUIRED, description = "The text of the note")
  val text: String
}
