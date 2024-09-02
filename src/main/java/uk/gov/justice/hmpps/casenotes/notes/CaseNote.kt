package uk.gov.justice.hmpps.casenotes.notes

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Schema(description = "Case Note")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaseNote(
  @Schema(required = true, description = "Case Note Id (unique)", example = "12311312")
  val caseNoteId: String,

  @Schema(required = true, description = "Offender Unique Identifier", example = "A1234AA")
  val offenderIdentifier: String,

  @Schema(required = true, description = "Case Note Type", example = "KA")
  @NotBlank
  val type: String,

  @Schema(required = true, description = "Case Note Type Description", example = "Key Worker")
  @NotBlank
  val typeDescription: String,

  @Schema(required = true, description = "Case Note Sub Type", example = "KS")
  @NotBlank
  val subType: String,

  @Schema(required = true, description = "Case Note Sub Type Description", example = "Key Worker Session")
  @NotBlank
  val subTypeDescription: String,

  @Schema(required = true, description = "Source Type", example = "INST")
  @NotBlank
  val source: String,

  @Schema(required = true, description = "Date and Time of Case Note creation", example = "2017-10-31T01:30:00")
  @NotNull
  val creationDateTime: LocalDateTime,

  @Schema(
    required = true,
    description = "Date and Time of when case note contact with offender was made",
    example = "2017-10-31T01:30:00",
  )
  val occurrenceDateTime: LocalDateTime,

  @Schema(required = true, description = "Full name of case note author", example = "John Smith")
  val authorName: String,

  @Schema(
    required = true,
    description = "User Id of case note author - staffId for nomis users, username for auth users",
    example = "12345 or USERNAME_12345",
  )
  val authorUserId: String,

  @Schema(required = true, description = "Case Note Text", example = "This is some text")
  @NotBlank
  val text: String,

  @Schema(description = "Location Id representing where Case Note was made.", example = "MDI")
  val locationId: String? = null,

  @Schema(
    required = true,
    description = "Delius number representation of the case note id - will be negative for sensitive case note types",
    example = "-23",
  )
  val eventId: Int,

  @Schema(required = true, description = "Sensitive Note", example = "true")
  val sensitive: Boolean = false,

  @Schema(required = true, description = "Ordered list of amendments to the case note (oldest first)")
  val amendments: List<CaseNoteAmendment> = listOf(),

  @Schema(required = true, description = "Flag to indicate if the case note was system generated or not")
  val systemGenerated: Boolean,

  @Schema(description = "A temporary field that holds the legacy (nomis) id for services that have a dependency on the legacy id")
  val legacyId: Long?,
) {

  class Builder internal constructor() {
    private var caseNoteId: @NotNull String? = null
    private var offenderIdentifier: @NotNull String? = null
    private var type: @NotBlank String? = null
    private var typeDescription: @NotBlank String? = null
    private var subType: @NotBlank String? = null
    private var subTypeDescription: @NotBlank String? = null
    private var source: @NotBlank String? = null
    private var creationDateTime: @NotNull LocalDateTime? = null
    private var occurrenceDateTime: @NotNull LocalDateTime? = null
    private var authorName: @NotNull String? = null
    private var authorUserId: @NotNull String? = null
    private var text: @NotBlank String? = null
    private var locationId: String? = null
    private var eventId: Int = 0
    private var sensitive = false
    private var amendments: List<CaseNoteAmendment> = listOf()
    private var systemGenerated = false
    private var legacyId: Long? = null

    fun caseNoteId(caseNoteId: String): Builder = apply {
      this.caseNoteId = caseNoteId
    }

    fun offenderIdentifier(offenderIdentifier: String): Builder = apply {
      this.offenderIdentifier = offenderIdentifier
    }

    fun type(@NotBlank type: String): Builder = apply {
      this.type = type
    }

    fun typeDescription(@NotBlank typeDescription: String): Builder = apply {
      this.typeDescription = typeDescription
    }

    fun subType(@NotBlank subType: String): Builder = apply {
      this.subType = subType
    }

    fun subTypeDescription(@NotBlank subTypeDescription: String): Builder = apply {
      this.subTypeDescription = subTypeDescription
    }

    fun source(@NotBlank source: String): Builder = apply {
      this.source = source
    }

    fun creationDateTime(creationDateTime: LocalDateTime): Builder = apply {
      this.creationDateTime = creationDateTime
    }

    fun occurrenceDateTime(occurrenceDateTime: LocalDateTime): Builder = apply {
      this.occurrenceDateTime = occurrenceDateTime
    }

    fun authorName(authorName: String): Builder = apply {
      this.authorName = authorName
    }

    fun authorUserId(authorUserId: String): Builder = apply {
      this.authorUserId = authorUserId
    }

    fun text(@NotBlank text: String): Builder = apply {
      this.text = text
    }

    fun locationId(locationId: String?): Builder = apply {
      this.locationId = locationId
    }

    fun eventId(eventId: Int): Builder = apply {
      this.eventId = eventId
    }

    fun sensitive(sensitive: Boolean): Builder = apply {
      this.sensitive = sensitive
    }

    fun amendments(amendments: List<CaseNoteAmendment>): Builder = apply {
      this.amendments = amendments
    }

    fun systemGenerated(systemGenerated: Boolean): Builder = apply {
      this.systemGenerated = systemGenerated
    }

    fun legacyId(legacyId: Number?): Builder = apply {
      this.legacyId = legacyId?.toLong()
    }

    fun build(): CaseNote {
      return CaseNote(
        caseNoteId!!,
        offenderIdentifier!!,
        type!!,
        typeDescription!!,
        subType!!,
        subTypeDescription!!,
        source!!,
        creationDateTime!!,
        occurrenceDateTime!!,
        authorName!!,
        authorUserId!!,
        text!!,
        locationId,
        eventId,
        sensitive,
        amendments,
        systemGenerated,
        legacyId,
      )
    }
  }

  companion object {
    @JvmStatic
    fun builder(): Builder {
      return Builder()
    }
  }
}
