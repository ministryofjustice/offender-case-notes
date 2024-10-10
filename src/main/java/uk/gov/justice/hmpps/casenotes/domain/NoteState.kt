package uk.gov.justice.hmpps.casenotes.domain

import java.time.LocalDateTime
import java.util.SortedSet
import java.util.UUID

interface NoteState {
  val personIdentifier: String
  val subTypeId: Long
  val occurredAt: LocalDateTime
  val locationId: String
  val authorUsername: String
  val authorUserId: String
  val authorName: String
  val text: String
  val systemGenerated: Boolean
  val system: System
  val legacyId: Long?
  val createdAt: LocalDateTime
  val createdBy: String
  fun getId(): UUID
  fun amendments(): SortedSet<out AmendmentState>
}

interface AmendmentState {
  val authorUsername: String
  val authorName: String
  val authorUserId: String
  val text: String
  val system: System
  val createdAt: LocalDateTime
  val createdBy: String
  fun getId(): UUID
}

enum class System {
  DPS,
  NOMIS,
}
