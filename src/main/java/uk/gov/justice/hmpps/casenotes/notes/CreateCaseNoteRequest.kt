package uk.gov.justice.hmpps.casenotes.notes

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import java.time.LocalDateTime

data class CreateCaseNoteRequest(
  @Schema(required = false, example = "MDI", description = "Location where case note was made")
  @field:Length(max = 6, message = "location must be no more than 6 characters")
  var locationId: String? = null,

  @Schema(required = true, description = "Type of case note", example = "GEN")
  @field:Length(max = 12, message = "type must be no more than 12 characters")
  @field:NotBlank(message = "type must not be blank")
  val type: String,

  @Schema(required = true, description = "Sub Type of case note", example = "OBS")
  @field:Length(max = 12, message = "sub type must be no more than 12 characters")
  @field:NotBlank(message = "sub type must not be blank")
  val subType: String,

  @Schema(
    required = false,
    example = "2019-01-17T10:25:00",
    description = "Occurrence time of case note. If not provided it will be defaulted to the time of the request.",
  )
  val occurrenceDateTime: LocalDateTime? = null,

  @Schema(required = true, description = "Text of case note", example = "This is a case note message")
  @field:Length(max = 30000)
  @field:NotBlank(message = "text cannot be blank")
  val text: String,

  @Schema(description = "Boolean flag to indicate if case not is system generated")
  var systemGenerated: Boolean?,
)
