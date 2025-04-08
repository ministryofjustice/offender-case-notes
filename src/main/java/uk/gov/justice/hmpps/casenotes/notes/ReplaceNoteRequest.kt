package uk.gov.justice.hmpps.casenotes.notes

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import java.util.UUID

data class ReplaceNoteRequest(
  override val type: String,
  override val subType: String,
  @field:NotBlank(message = "note text cannot be blank")
  override val text: String,
  override val occurrenceDateTime: LocalDateTime,
  @field:NotBlank(message = "reason cannot be blank") val reason: String,
  @field:Valid val amendments: List<ReplaceAmendmentRequest> = listOf(),
) : TypeAndSubTypeRequest,
  OccurredAtRequest,
  TextRequest

data class ReplaceAmendmentRequest(
  val id: UUID,
  @field:NotBlank(message = "amendment text cannot be blank")
  override val text: String,
) : TextRequest
