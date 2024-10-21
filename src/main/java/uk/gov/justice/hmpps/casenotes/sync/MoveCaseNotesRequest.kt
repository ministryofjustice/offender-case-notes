package uk.gov.justice.hmpps.casenotes.sync

import java.util.UUID

data class MoveCaseNotesRequest(
  val fromPersonIdentifier: String,
  val toPersonIdentifier: String,
  val caseNoteIds: Set<UUID>,
)
