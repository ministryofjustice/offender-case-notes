package uk.gov.justice.hmpps.casenotes.systemgenerated

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.hmpps.casenotes.notes.AuthoredRequest
import uk.gov.justice.hmpps.casenotes.notes.NoteRequest
import java.time.LocalDateTime

data class SystemGeneratedRequest(
  override val locationId: String?,
  override val type: String,
  override val subType: String,
  override val occurrenceDateTime: LocalDateTime? = null,
  override val authorUsername: String? = null,
  override val authorName: String,
  @field:NotBlank(message = "text cannot be blank")
  override val text: String,
) : NoteRequest, AuthoredRequest {
  @JsonIgnore
  override val systemGenerated: Boolean = true
}
