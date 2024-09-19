package uk.gov.justice.hmpps.casenotes.notes

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class CreateCaseNoteRequest(
  override var locationId: String? = null,
  override val type: String,
  override val subType: String,
  override val occurrenceDateTime: LocalDateTime? = null,
  @field:NotBlank(message = "text cannot be blank")
  override val text: String,
  override var systemGenerated: Boolean?,
) : NoteRequest
