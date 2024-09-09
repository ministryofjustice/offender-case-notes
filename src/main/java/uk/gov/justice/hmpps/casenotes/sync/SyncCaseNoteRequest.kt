package uk.gov.justice.hmpps.casenotes.sync

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.notes.AuthoredRequest
import uk.gov.justice.hmpps.casenotes.notes.TextRequest
import java.time.LocalDateTime
import java.util.UUID

data class SyncCaseNoteRequest(
  @Schema(
    requiredMode = REQUIRED,
    example = "1645251",
    description = "The nomis id for the case note, reflected by for mapping",
  )
  val legacyId: Long,

  @Schema(
    example = "c9475622-676f-4659-8bb5-12a4760280d7",
    description = "The id for the case note, if provided, the existing case note matching this id will be updated, otherwise a new case note is created.",
  )
  val id: UUID?,

  @Schema(
    requiredMode = REQUIRED,
    example = "A1234BC",
    description = "The offender/prison/prisoner/noms number - used to identify the person in prison",
  )
  val personIdentifier: String,

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
  val occurrenceDateTime: LocalDateTime,

  @Schema(requiredMode = REQUIRED, description = "Text of case note", example = "This is a case note message")
  @NotBlank
  override val text: String,

  @Schema(requiredMode = REQUIRED, description = "Boolean flag to indicate if case not is system generated")
  val systemGenerated: Boolean,

  @Schema(requiredMode = REQUIRED, description = "Username of the staff member that created the case note")
  @NotBlank
  override val authorUsername: String,

  @Schema(requiredMode = REQUIRED, description = "Id of the staff member that created the case note")
  @NotBlank
  override val authorUserId: String,

  @Schema(requiredMode = REQUIRED, description = "Full name of the staff member that created the case note")
  @NotBlank
  override val authorName: String,

  @Schema(
    requiredMode = REQUIRED,
    example = "2024-09-01T10:25:00",
    description = "Created date time from audit field in nomis to allow ordering by creation date",
  )
  override val createdDateTime: LocalDateTime,

  @Schema(requiredMode = REQUIRED, description = "Username of the user that created the case note")
  @NotBlank
  val createdByUsername: String,

  @Schema(
    requiredMode = REQUIRED,
    example = "DPS",
    allowableValues = ["DPS", "NOMIS"],
    description = "Indicates whether the case note was created via prison API or nomis",
  )
  val source: Source,

  @Schema(description = "Amendments to the original case note")
  val amendments: Set<SyncAmendmentRequest>,
) : TextRequest, AuthoredRequest

data class SyncAmendmentRequest(
  @Schema(requiredMode = REQUIRED, description = "Text of the amendment")
  @NotBlank
  override val text: String,

  @Schema(requiredMode = REQUIRED, description = "Username of the staff member that amended the case note")
  @NotBlank
  override val authorUsername: String,

  @Schema(requiredMode = REQUIRED, description = "Id of the staff member that amended the case note")
  @NotBlank
  override val authorUserId: String,

  @Schema(requiredMode = REQUIRED, description = "Full name of the staff member that amended the case note")
  @NotBlank
  override val authorName: String,

  @Schema(requiredMode = REQUIRED, example = "2024-09-01T10:25:00", description = "Date time of the amendment")
  override val createdDateTime: LocalDateTime,
) : TextRequest, AuthoredRequest
