package uk.gov.justice.hmpps.casenotes.notes

import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.Note

const val SOURCE_OCNS = "OCNS"
const val SOURCE_AUTO = "AUTO"
const val SOURCE_INST = "INST"

internal fun Note.toModel() = CaseNote(
  id = id.toString(),
  personIdentifier = personIdentifier,
  type = subType.type.code,
  typeDescription = subType.type.description,
  subType = subType.code,
  subTypeDescription = subType.description,
  source = when {
    !subType.syncToNomis -> SOURCE_OCNS
    !subType.dpsUserSelectable -> SOURCE_AUTO
    else -> SOURCE_INST
  },
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
