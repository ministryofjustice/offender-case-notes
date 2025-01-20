package uk.gov.justice.hmpps.casenotes.sync

import java.time.LocalDateTime
import java.util.UUID

data class ResendPersonCaseNoteEvents(
  val uuids: Set<UUID> = setOf(),
  val createdBetween: CreatedBetween? = null,
)

data class CreatedBetween(val from: LocalDateTime, val to: LocalDateTime, val includeSyncToNomis: Boolean = false)
