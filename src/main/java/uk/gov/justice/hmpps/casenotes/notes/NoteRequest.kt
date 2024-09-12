package uk.gov.justice.hmpps.casenotes.notes

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import java.time.LocalDateTime

interface NoteRequest : TextRequest {
  @get:Schema(example = "MDI", description = "Location where case note was made")
  @get:Length(max = 12, message = "location must be no more than 12 characters")
  val locationId: String?

  @get:Schema(requiredMode = REQUIRED, description = "Type of case note", example = "GEN")
  @get:Length(max = 12, message = "type must be no more than 12 characters")
  @get:NotBlank(message = "type must not be blank")
  val type: String

  @get:Schema(requiredMode = REQUIRED, description = "Sub Type of case note", example = "OBS")
  @get:Length(max = 12, message = "sub type must be no more than 12 characters")
  @get:NotBlank(message = "sub type must not be blank")
  val subType: String

  @get:Schema(
    example = "2019-01-17T10:25:00",
    description = "Occurrence time of case note. If not provided it will be defaulted to the time of the request.",
  )
  val occurrenceDateTime: LocalDateTime?

  @get:Schema(description = "Boolean flag to indicate if case not is system generated")
  val systemGenerated: Boolean?
}
