package uk.gov.justice.hmpps.casenotes.notes

import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.notes.ReadCaseNote.Companion.SOURCE

internal fun Note.toModel() = CaseNote(
  caseNoteId = id.toString(),
  personIdentifier = personIdentifier,
  type = type.parent.code,
  typeDescription = type.parent.description,
  subType = type.code,
  subTypeDescription = type.description,
  source = SOURCE,
  createdAt = createdAt,
  occurredAt = occurredAt,
  authorName = authorName,
  authorUserId = authorUserId,
  text = text,
  locationId = locationId,
  eventId = legacyId,
  sensitive = type.sensitive,
  systemGenerated = systemGenerated,
  legacyId = legacyId,
  amendments = amendments().map { it.toModel() },
)

internal fun Amendment.toModel() =
  CaseNoteAmendment(createdAt, authorUsername, authorName, authorUserId, text)
