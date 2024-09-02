package uk.gov.justice.hmpps.casenotes.notes.internal

import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.notes.internal.ReadCaseNote.Companion.SOURCE

internal fun Note.toModel() = CaseNote(
  caseNoteId = id.toString(),
  offenderIdentifier = prisonNumber,
  type = type.category.code,
  typeDescription = type.category.description,
  subType = type.code,
  subTypeDescription = type.description,
  source = SOURCE,
  creationDateTime = createDateTime,
  occurrenceDateTime = occurredAt,
  authorName = authorName,
  authorUserId = authorUserId,
  text = text,
  locationId = locationId,
  eventId = requireNotNull(eventId),
  sensitive = type.sensitive,
  systemGenerated = systemGenerated,
  legacyId = legacyId,
  amendments = amendments().map { it.toModel() },
)

internal fun Amendment.toModel() =
  CaseNoteAmendment(id!!, createDateTime, authorUsername, authorName, authorUserId, text)
