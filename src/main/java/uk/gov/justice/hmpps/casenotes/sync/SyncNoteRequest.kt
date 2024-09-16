package uk.gov.justice.hmpps.casenotes.sync

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.notes.AuthoredRequest
import uk.gov.justice.hmpps.casenotes.notes.NoteRequest
import uk.gov.justice.hmpps.casenotes.notes.TextRequest
import uk.gov.justice.hmpps.casenotes.utils.LanguageFormatUtils
import java.time.LocalDateTime

interface SyncNoteRequest : NoteRequest, AuthoredRequest {
  @get:Schema(
    requiredMode = REQUIRED,
    example = "1645251",
    description = "The nomis id for the case note, reflected back for mapping",
  )
  val legacyId: Long

  @get:Schema(
    requiredMode = REQUIRED,
    example = "A1234BC",
    description = "The offender/prison/prisoner/noms number - used to identify the person in prison",
  )
  @get:Length(max = 12, message = "person identifier cannot be more than 12 characters")
  @get:NotBlank(message = "person identifier cannot be blank")
  val personIdentifier: String
  override val locationId: String

  @get:Schema(
    requiredMode = REQUIRED,
    example = "2024-09-01T10:25:00",
    description = "Occurrence time of case note. If not provided it will be defaulted to the time of the request.",
  )
  override val occurrenceDateTime: LocalDateTime

  @get:Schema(hidden = true)
  override val authorUsername: String
    get() = author.username

  @get:Schema(hidden = true)
  override val authorName: String
    get() = LanguageFormatUtils.formatDisplayName("${author.firstName} ${author.lastName}").trim()

  @get:Schema(hidden = true)
  val authorUserId: String
    get() = author.userId

  @get:Schema(description = "The staff member who created the case note")
  @get:Valid
  val author: CreateAuthor

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

interface SyncAmendmentRequest : TextRequest, AuthoredRequest {
  @get:Schema(hidden = true)
  override val authorUsername: String
    get() = author.username

  @get:Schema(hidden = true)
  override val authorName: String
    get() = LanguageFormatUtils.formatDisplayName("${author.firstName} ${author.lastName}").trim()

  @get:Schema(hidden = true)
  val authorUserId: String
    get() = author.userId

  @get:Schema(description = "The staff member who amended the case note")
  @get:Valid
  val author: AmendAuthor
  val createdDateTime: LocalDateTime
}

data class CreateAuthor(
  @get:Schema(requiredMode = REQUIRED, description = "Full name of the staff member that created the case note")
  @get:Length(max = 80, message = "author name cannot be more than 80 characters")
  @get:NotBlank(message = "author name cannot be blank")
  val username: String,
  @get:Schema(requiredMode = REQUIRED, description = "Id of the staff member that created the case note")
  @get:Length(max = 64, message = "author user id cannot be more than 64 characters")
  @get:NotBlank(message = "author user id cannot be blank")
  val userId: String,
  @get:Schema(requiredMode = REQUIRED, description = "First name of the staff member that created the case note")
  @get:NotBlank(message = "author first name cannot be blank")
  val firstName: String,
  @get:Schema(description = "Last name of the staff member that created the case note")
  val lastName: String,
)

data class AmendAuthor(
  @get:NotBlank(message = "author username cannot be blank")
  val username: String,
  @get:Length(max = 64, message = "author user id cannot be more than 64 characters")
  @get:NotBlank(message = "author user id cannot be blank")
  val userId: String,
  @get:NotBlank(message = "author first name cannot be blank")
  val firstName: String,
  val lastName: String,
)
