package uk.gov.justice.hmpps.casenotes.domain

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.LocalDateTime
import java.util.SortedSet
import java.util.UUID

@JsonNaming(SnakeCaseStrategy::class)
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
  fun amendments(): SortedSet<out AmendmentState>
}

@JsonNaming(SnakeCaseStrategy::class)
interface AmendmentState {
  val authorUsername: String
  val authorName: String
  val authorUserId: String
  val text: String
  val id: UUID
}