package uk.gov.justice.hmpps.casenotes.notes

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Schema(description = "Case Note Amendment")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaseNoteAmendment(
  @Schema(required = true, description = "Date and Time of Case Note creation", example = "2018-12-01T13:45:00")
  val creationDateTime: LocalDateTime,

  @Schema(required = true, description = "Username of the user amending the case note", example = "USER1")
  @NotBlank val authorUserName: String,

  @Schema(required = true, description = "Name of the user amending the case note", example = "Mickey Mouse")
  @NotBlank val authorName: String,

  @Schema(
    required = false,
    example = "12345",
    description = "User Id of the user amending the case note - staffId for nomis users, userId for auth users",
  )
  val authorUserId: String?,

  @Schema(required = true, description = "Additional Case Note Information", example = "Some Additional Text")
  @NotBlank val additionalNoteText: String,
)
