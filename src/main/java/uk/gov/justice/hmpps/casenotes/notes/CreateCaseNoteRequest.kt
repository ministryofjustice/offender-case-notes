package uk.gov.justice.hmpps.casenotes.notes

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import java.time.LocalDateTime

data class CreateCaseNoteRequest(
  @Schema(
    required = true,
    example = "MDI",
    description = "Location where case note was made",
  )
  val locationId:
  @Length(max = 6)
  String? = null,

  @Schema(required = true, description = "Type of case note", example = "GEN")
  val type:
  @Length(max = 12)
  @NotBlank
  String,

  @Schema(required = true, description = "Sub Type of case note", example = "OBS")
  val subType:
  @Length(max = 12)
  @NotBlank
  String,

  @Schema(required = true, description = "Occurrence time of case note", example = "2019-01-17T10:25:00")
  val occurrenceDateTime: LocalDateTime = LocalDateTime.now(),

  @Schema(required = true, description = "Text of case note", example = "This is a case note message")
  val text:
  @Length(max = 30000)
  @NotBlank
  String,

  @Schema(description = "Boolean flag to indicate if case not is system generated")
  var systemGenerated: Boolean?,
)
