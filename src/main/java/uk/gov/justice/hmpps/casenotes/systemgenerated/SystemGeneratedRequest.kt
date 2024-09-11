package uk.gov.justice.hmpps.casenotes.systemgenerated

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.Length
import uk.gov.justice.hmpps.casenotes.notes.AuthoredRequest
import uk.gov.justice.hmpps.casenotes.notes.TextRequest
import java.time.LocalDateTime

data class SystemGeneratedRequest(
  @Schema(
    example = "MDI",
    description = "Location where case note was made. If not provided, location of person will be retrieved using prison search.",
  )
  @field:Length(max = 6, message = "location must be no more than 6 characters")
  val locationId: String?,

  @Schema(requiredMode = REQUIRED, description = "Type of case note", example = "GEN")
  @field:Length(max = 12, message = "type must be no more than 12 characters")
  @field:NotBlank(message = "type cannot be blank")
  val type: String,

  @Schema(requiredMode = REQUIRED, description = "Sub Type of case note", example = "OBS")
  @field:Length(max = 12, message = "sub type must be no more than 12 characters")
  @field:NotBlank(message = "sub type cannot be blank")
  val subType: String,

  @Schema(
    requiredMode = REQUIRED,
    example = "2024-09-01T10:25:00",
    description = "Occurrence time of case note. If not provided it will be defaulted to the time of the request.",
  )
  val occurrenceDateTime: LocalDateTime? = null,

  @Schema(description = "Optional username of the staff member that created the case note. If not provided, the subject of the jwt token used to authorise the request will be used.")
  @field:Size(max = 64, message = "author username cannot be more than 64 characters")
  override val authorUsername: String? = null,

  @Schema(requiredMode = REQUIRED, description = "Full name of the staff member that created the case note")
  @field:Size(max = 80, message = "author name cannot be more than 80 characters")
  @field:NotBlank(message = "author name cannot be blank")
  override val authorName: String,

  @Schema(requiredMode = REQUIRED, description = "Text of case note", example = "This is a case note message")
  @field:NotBlank(message = "text cannot be blank")
  override val text: String,

) : TextRequest, AuthoredRequest
