package uk.gov.justice.hmpps.casenotes.sync

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.notes.NoteRequest
import uk.gov.justice.hmpps.casenotes.notes.TextRequest
import java.time.LocalDateTime

interface SyncNoteRequest : NoteRequest {
  @get:Schema(
    requiredMode = REQUIRED,
    example = "1645251",
    description = "The nomis id for the case note, reflected back for mapping",
  )
  val legacyId: Long
  override val locationId: String

  @get:Schema(
    requiredMode = REQUIRED,
    example = "2024-09-01T10:25:00",
    description = "Occurrence time of case note. If not provided it will be defaulted to the time of the request.",
  )
  override val occurrenceDateTime: LocalDateTime

  @get:Schema(requiredMode = REQUIRED, description = "The details of the author of the case note")
  @get:Valid
  val author: Author

  @get:Schema(
    requiredMode = REQUIRED,
    example = "2024-09-01T10:25:00",
    description = "Created date time from audit field in nomis to allow ordering by creation date",
  )
  val createdDateTime: LocalDateTime

  @get:Schema(requiredMode = REQUIRED, description = "Username of the user that created the case note")
  @get:NotBlank(message = "created by username cannot be blank")
  val createdByUsername: String

  @get:Schema(
    requiredMode = REQUIRED,
    example = "DPS",
    allowableValues = ["DPS", "NOMIS"],
    description = "Indicates whether the case note was created via prison API or nomis",
  )
  val source: Source

  @get:Schema(requiredMode = REQUIRED, description = "Boolean flag to indicate if case note is system generated")
  override val systemGenerated: Boolean

  @get:Schema(description = "Amendments to the original case note")
  @get:Valid
  val amendments: Set<SyncAmendmentRequest>
}

interface SyncAmendmentRequest : TextRequest {
  @get:Valid
  val author: Author
  val createdDateTime: LocalDateTime
}

data class Author(
  @field:Schema(requiredMode = REQUIRED, description = "Username of the staff member that created the case note")
  @field:Length(max = 64, message = "author username cannot be more than 64 characters")
  @field:NotBlank(message = "author username cannot be blank")
  val username: String,
  @field:Schema(requiredMode = REQUIRED, description = "Id of the staff member that created the case note")
  @field:Length(max = 64, message = "author user id cannot be more than 64 characters")
  @field:NotBlank(message = "author user id cannot be blank")
  val userId: String,
  @field:Schema(requiredMode = REQUIRED, description = "The first name of the author")
  @field:NotBlank(message = "author first name cannot be blank")
  val firstName: String,
  @field:Schema(requiredMode = REQUIRED, description = "The last name of the author")
  @field:NotBlank(message = "author last name cannot be blank")
  val lastName: String,
) {
  fun fullName(): String = "${firstName.capitalised()} ${lastName.capitalised()}"

  private fun String.capitalised(): String {
    return this.lowercase().replace("[^\\pL]+\\pL".toRegex()) {
      it.value.uppercase()
    }.replaceFirstChar { it.uppercase() }
  }
}
