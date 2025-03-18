package uk.gov.justice.hmpps.casenotes.notes

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Case Note Amendment")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaseNoteAmendment(
  @JsonProperty("creationDateTime")
  @Schema(required = false, description = "Date and time the amendment to the case note was added", example = "2018-12-01T13:45:00")
  val createdAt: LocalDateTime?,

  @Schema(required = true, description = "Username of the user amending the case note", example = "USER1")
  val authorUserName: String,

  @Schema(required = true, description = "Name of the user amending the case note", example = "Mickey Mouse")
  val authorName: String,

  @Schema(
    required = false,
    example = "12345",
    description = "User identifier of the user amending the case note - staffId for NOMIS users, userId for auth users",
  )
  val authorUserId: String?,

  @Schema(required = true, description = "The text of the case note amendment", example = "Some Additional Text")
  val additionalNoteText: String,
)
