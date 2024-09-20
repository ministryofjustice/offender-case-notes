package uk.gov.justice.hmpps.casenotes.events

import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.Note
import java.time.ZoneId
import java.util.UUID

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
) {
  enum class Type {
    CREATED,
    UPDATED,
    DELETED,
  }

  val eventName = EVENT_PREFIX + eventType.name.lowercase()
  val detailUrl = "/case-notes/$personIdentifier/$id"

  companion object {
    private const val EVENT_PREFIX = "person.case-note."

    fun Note.createEvent(eventType: Type): PersonCaseNoteEvent = PersonCaseNoteEvent(
      eventType,
      personIdentifier,
      id,
      legacyId,
      subType.type.code,
      subType.code,
      CaseNoteRequestContext.get().source,
      subType.syncToNomis,
      systemGenerated,
    )
  }
}

fun PersonCaseNoteEvent.asDomainEvent(baseUrl: String): DomainEvent = DomainEvent(
  CaseNoteRequestContext.get().requestAt.atZone(ZoneId.systemDefault()),
  eventName,
  baseUrl + detailUrl,
  description = "A case note has been ${eventType.name.lowercase()}",
  AdditionalInformation(id, legacyId, type, subType, source, syncToNomis, systemGenerated),
  PersonReference.withIdentifier(personIdentifier),
)
