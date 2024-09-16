package uk.gov.justice.hmpps.casenotes.domain

import java.time.LocalDateTime
import java.util.SortedSet
import java.util.UUID

interface NoteState {
  val personIdentifier: String
  val typeId: Long
  val occurredAt: LocalDateTime
  val locationId: String
  val authorUsername: String
  val authorUserId: String
  val authorName: String
  val text: String
  val systemGenerated: Boolean
  val legacyId: Long?
  val id: UUID
  val createdAt: LocalDateTime
  val createdBy: String
  fun amendments(): SortedSet<out AmendmentState>
}

interface AmendmentState {
  val authorUsername: String
  val authorName: String
  val authorUserId: String
  val text: String
  val id: UUID
  val createdAt: LocalDateTime
  val createdBy: String
}
