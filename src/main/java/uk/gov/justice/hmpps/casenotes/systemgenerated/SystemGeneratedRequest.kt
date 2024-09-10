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

  @Schema(requiredMode = REQUIRED, example = "MDI", description = "Location where case note was made")
  @Length(max = 6)
  @NotBlank
  val locationId: String,

  @Schema(requiredMode = REQUIRED, description = "Type of case note", example = "GEN")
  @Length(max = 12)
  @NotBlank
  val type: String,

  @Schema(requiredMode = REQUIRED, description = "Sub Type of case note", example = "OBS")
  @Length(max = 12)
  @NotBlank
  val subType: String,

  @Schema(
    requiredMode = REQUIRED,
    example = "2024-09-01T10:25:00",
    description = "Occurrence time of case note. If not provided it will be defaulted to the time of the request.",
  )
  val occurrenceDateTime: LocalDateTime? = null,

  @Schema(description = "Optional username of the staff member that created the case note. If not provided, the subject of the jwt token used to authorise the request will be used.")
  @field:Size(max = 64, message = "Author username cannot be more than 64 characters")
  @NotBlank
  override val authorUsername: String? = null,

  @Schema(requiredMode = REQUIRED, description = "Full name of the staff member that created the case note")
  @field:Size(max = 80, message = "Author name cannot be more than 80 characters")
  @NotBlank
  override val authorName: String,

  @Schema(requiredMode = REQUIRED, description = "Text of case note", example = "This is a case note message")
  @NotBlank
  override val text: String,

) : TextRequest, AuthoredRequest
