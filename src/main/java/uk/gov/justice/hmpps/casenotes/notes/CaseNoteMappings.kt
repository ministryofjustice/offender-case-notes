package uk.gov.justice.hmpps.casenotes.notes

import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.notes.ReadCaseNote.Companion.SOURCE

internal fun Note.toModel() = CaseNote(
  id = id.toString(),
  personIdentifier = personIdentifier,
  type = subType.type.code,
  typeDescription = subType.type.description,
  subType = subType.code,
  subTypeDescription = subType.description,
  source = SOURCE,
  createdAt = createdAt,
  occurredAt = occurredAt,
  authorName = authorName,
  authorUserId = authorUserId,
  authorUsername = authorUsername,
  text = text,
  locationId = locationId,
  eventId = legacyId,
  sensitive = subType.sensitive,
  systemGenerated = systemGenerated,
  legacyId = legacyId,
  amendments = amendments().map { it.toModel() },
)

internal fun Amendment.toModel() =
  CaseNoteAmendment(createdAt, authorUsername, authorName, authorUserId, text)
