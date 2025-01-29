package uk.gov.justice.hmpps.casenotes.events

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.Note
import java.time.ZoneId
import java.util.UUID

@JsonInclude(NON_NULL)
data class PersonCaseNoteEvent(
  val eventType: Type,
  val personIdentifier: String,
  val id: UUID,
  val legacyId: Long,
  val type: String,
  val subType: String,
  val source: Source,
  val syncToNomis: Boolean,
  val systemGenerated: Boolean,
  val previousPersonIdentifier: String?,
) {
  enum class Type {
    CREATED,
    UPDATED,
    DELETED,
    MOVED,
  }

  val eventName = EVENT_PREFIX + eventType.name.lowercase()
  val detailUrl = "/case-notes/$personIdentifier/$id"

  companion object {
    private const val EVENT_PREFIX = "person.case-note."

    fun Note.createEvent(
      eventType: Type,
      previousPersonIdentifier: String? = null,
      sourceOverride: Source? = null,
    ): PersonCaseNoteEvent = PersonCaseNoteEvent(
      eventType,
      personIdentifier,
      id,
      legacyId,
      subType.type.code,
      subType.code,
      sourceOverride ?: CaseNoteRequestContext.get().source,
      subType.syncToNomis,
      systemGenerated,
      previousPersonIdentifier,
    )
  }
}

fun PersonCaseNoteEvent.asDomainEvent(baseUrl: String): DomainEvent<CaseNoteInformation> = DomainEvent(
  CaseNoteRequestContext.get().requestAt.atZone(ZoneId.systemDefault()),
  eventName,
  baseUrl + detailUrl,
  description = "A case note has been ${eventType.name.lowercase()}",
  CaseNoteInformation(id, legacyId, type, subType, source, syncToNomis, systemGenerated, previousPersonIdentifier),
  PersonReference.withIdentifier(personIdentifier),
)
