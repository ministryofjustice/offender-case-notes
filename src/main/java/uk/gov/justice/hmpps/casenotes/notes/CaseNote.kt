package uk.gov.justice.hmpps.casenotes.notes

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import java.time.LocalDateTime

@Schema(description = "Case Note")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaseNote(
  @JsonProperty("caseNoteId")
  @Schema(
    requiredMode = REQUIRED,
    description = "Case Note Identifier. Will be a UUID for all consumers of the combined dataset but can be numeric for those that have not opted in. See documentation above",
    example = "518b2200-6489-4c77-8514-10cf80ccd488",
  )
  val id: String,

  @JsonProperty("offenderIdentifier")
  @Schema(
    requiredMode = REQUIRED,
    description = "Person identifier. Their assigned prison number also known as prisoner number, offender number, offender id or NOMS id",
    example = "A1234AA",
  )
  val personIdentifier: String,

  @Schema(requiredMode = REQUIRED, description = "The type code categorising the case note", example = "CAB")
  val type: String,

  @Schema(requiredMode = REQUIRED, description = "Description of the case note type", example = "Conduct & Behaviour")
  val typeDescription: String,

  @Schema(requiredMode = REQUIRED, description = "The sub-type code further categorising the case note", example = "EDUCATION")
  val subType: String,

  @Schema(requiredMode = REQUIRED, description = "Description of the case note sub-type", example = "Education")
  val subTypeDescription: String,

  @Schema(requiredMode = REQUIRED, deprecated = true, description = "Deprecated for removal", example = "INST|AUTO|OCNS")
  val source: String,

  @JsonProperty("creationDateTime")
  @Schema(requiredMode = REQUIRED, description = "Date and time the case note was added", example = "2017-10-31T01:30:00")
  val createdAt: LocalDateTime,

  @JsonProperty("occurrenceDateTime")
  @Schema(
    requiredMode = REQUIRED,
    description = "Date and time of the event recorded by this case note e.g. when a member of prison staff interacted with the prisoner. Displayed as 'Happened' in DPS",
    example = "2017-10-31T01:30:00",
  )
  val occurredAt: LocalDateTime,

  @Schema(requiredMode = REQUIRED, description = "Full name of case note author", example = "John Smith")
  val authorName: String,

  @Schema(
    requiredMode = REQUIRED,
    description = "User identifier of case note author - staffId for NOMIS users, username for auth users",
    example = "12345 or USERNAME_12345",
  )
  val authorUserId: String,

  @Schema(requiredMode = REQUIRED, description = "Username of the case note author", example = "USER1")
  val authorUsername: String,

  @Schema(
    requiredMode = REQUIRED,
    description = "The text of the case note only. The text for added amendments is associated with each amendment",
    example = "This is some text",
  )
  val text: String,

  @Schema(
    description = "The prison code the prisoner was resident at or a code indicating their transfer or released status at the time of the case note was added",
    example = "MDI",
  )
  val locationId: String? = null,

  @Schema(
    deprecated = true,
    requiredMode = REQUIRED,
    description = "Deprecated - replaced with legacyId",
    example = "-23",
  )
  val eventId: Long,

  @Schema(
    requiredMode = REQUIRED,
    description = "Whether the text contains potentially sensitive information. Sensitive notes should only be displayed to users with one of the `POM`, `VIEW_SENSITIVE_CASE_NOTES` or `ADD_SENSITIVE_CASE_NOTES` DPS roles",
    example = "true",
  )
  val sensitive: Boolean = false,

  @Schema(requiredMode = REQUIRED, description = "Ordered list of amendments added to the case note (oldest first)")
  val amendments: List<CaseNoteAmendment> = listOf(),

  @Schema(requiredMode = REQUIRED, description = "Flag to indicate if the case note was system generated or not")
  val systemGenerated: Boolean,

  @Schema(
    description = "The assigned numeric id for the case note. Can be positive or negative. Should only be used temporarily by clients that want to opt into the combined data set but are not fully compatible with UUID case note identifiers",
    deprecated = true,
  )
  val legacyId: Long,
) {

  class Builder internal constructor() {
    private var id: String? = null
    private var personIdentifier: String? = null
    private var type: String? = null
    private var typeDescription: String? = null
    private var subType: String? = null
    private var subTypeDescription: String? = null
    private var source: String? = null
    private var createdAt: LocalDateTime? = null
    private var occurredAt: LocalDateTime? = null
    private var authorName: String? = null
    private var authorUserId: String? = null
    private var authorUsername: String? = null
    private var text: String? = null
    private var locationId: String? = null
    private var eventId: Long = 0
    private var sensitive = false
    private var amendments: List<CaseNoteAmendment> = listOf()
    private var systemGenerated = false
    private var legacyId: Long = 0

    fun id(id: String): Builder = apply {
      this.id = id
    }

    fun personIdentifier(offenderIdentifier: String): Builder = apply {
      this.personIdentifier = offenderIdentifier
    }

    fun type(type: String): Builder = apply {
      this.type = type
    }

    fun typeDescription(typeDescription: String): Builder = apply {
      this.typeDescription = typeDescription
    }

    fun subType(subType: String): Builder = apply {
      this.subType = subType
    }

    fun subTypeDescription(subTypeDescription: String): Builder = apply {
      this.subTypeDescription = subTypeDescription
    }

    fun source(source: String): Builder = apply {
      this.source = source
    }

    fun createdAt(creationDateTime: LocalDateTime): Builder = apply {
      this.createdAt = creationDateTime
    }

    fun occurredAt(occurrenceDateTime: LocalDateTime): Builder = apply {
      this.occurredAt = occurrenceDateTime
    }

    fun authorName(authorName: String): Builder = apply {
      this.authorName = authorName
    }

    fun authorUserId(authorUserId: String): Builder = apply {
      this.authorUserId = authorUserId
    }

    fun authorUsername(authorUsername: String): Builder = apply {
      this.authorUsername = authorUsername
    }

    fun text(text: String): Builder = apply {
      this.text = text
    }

    fun locationId(locationId: String?): Builder = apply {
      this.locationId = locationId
    }

    fun eventId(eventId: Long): Builder = apply {
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

    fun legacyId(legacyId: Long): Builder = apply {
      this.legacyId = legacyId
    }

    fun build(): CaseNote = CaseNote(
      id!!,
      personIdentifier!!,
      type!!,
      typeDescription!!,
      subType!!,
      subTypeDescription!!,
      source!!,
      createdAt!!,
      occurredAt!!,
      authorName!!,
      authorUserId!!,
      authorUsername!!,
      text!!,
      locationId,
      eventId,
      sensitive,
      amendments,
      systemGenerated,
      legacyId,
    )
  }

  companion object {
    @JvmStatic
    fun builder(): Builder = Builder()
  }
}

fun String.asLegacyId() = toLongOrNull()
