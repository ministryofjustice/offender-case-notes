package uk.gov.justice.hmpps.casenotes.notes

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

interface AuthoredRequest {
  @get:Length(max = 64, message = "author username cannot be more than 64 characters")
  val authorUsername: String?

  @get:Schema(requiredMode = REQUIRED, description = "Full name of the staff member that created the case note")
  @get:Length(max = 80, message = "author name cannot be more than 80 characters")
  @get:NotBlank(message = "author name cannot be blank")
  val authorName: String
}
