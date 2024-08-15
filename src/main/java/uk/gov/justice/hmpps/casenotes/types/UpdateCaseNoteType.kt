package uk.gov.justice.hmpps.casenotes.types

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

data class UpdateParentType(
  @Schema(required = true, description = "Type Description", example = "General Note Type")
  @field:Length(max = 80)
  @field:NotBlank
  val description: String,
)

data class UpdateSubType(
  @Schema(required = true, description = "Type Description", example = "General Note Type")
  @field:Length(max = 80)
  @field:NotBlank
  val description: String,

  @Schema(description = "Active Type, default true", example = "true")
  val active: Boolean = true,

  @Schema(description = "Sensitive Case Note Type, default true", example = "true")
  val sensitive: Boolean = true,

  @Schema(description = "Restricted Use, default true", example = "true")
  val restrictedUse: Boolean = true,
)