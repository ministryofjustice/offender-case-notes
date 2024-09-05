package uk.gov.justice.hmpps.casenotes.notes

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import java.time.LocalDateTime

data class CreateCaseNoteRequest(
  @Schema(required = false, example = "MDI", description = "Location where case note was made")
  @Length(max = 6)
  var locationId: String? = null,

  @Schema(required = true, description = "Type of case note", example = "GEN")
  @Length(max = 12)
  @NotBlank
  val type: String,

  @Schema(required = true, description = "Sub Type of case note", example = "OBS")
  @Length(max = 12)
  @NotBlank
  val subType: String,

  @Schema(
    required = false,
    example = "2019-01-17T10:25:00",
    description = "Occurrence time of case note. If not provided it will be defaulted to the time of the request.",
  )
  val occurrenceDateTime: LocalDateTime? = null,

  @Schema(required = true, description = "Text of case note", example = "This is a case note message")
  @Length(max = 30000)
  @NotBlank
  val text: String,

  @Schema(description = "Boolean flag to indicate if case not is system generated")
  var systemGenerated: Boolean?,
)
