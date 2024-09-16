package uk.gov.justice.hmpps.casenotes.sync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.IdGenerator.newUuid
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.TypeKey
import java.time.LocalDateTime
import java.util.UUID

data class SyncCaseNoteRequest(
  override val legacyId: Long,

  @Schema(
    example = "c9475622-676f-4659-8bb5-12a4760280d7",
    description = "The id for the case note, if provided, the existing case note matching this id will be updated, otherwise a new case note is created.",
  )
  val id: UUID?,

  override val personIdentifier: String,
  override val locationId: String,
  override val type: String,
  override val subType: String,
  override val occurrenceDateTime: LocalDateTime,
  override val text: String,
  override val systemGenerated: Boolean,
  override val authorUsername: String,
  override val authorUserId: String,
  override val authorName: String,
  override val createdDateTime: LocalDateTime,
  override val createdByUsername: String,
  override val source: Source,
  override val amendments: Set<SyncCaseNoteAmendmentRequest>,
) : SyncNoteRequest

data class SyncCaseNoteAmendmentRequest(
  override val text: String,
  override val authorUsername: String,
  override val authorUserId: String,
  override val authorName: String,
  override val createdDateTime: LocalDateTime,
) : SyncAmendmentRequest

internal data class NoteAndAmendments(val note: Note, val amendments: List<Amendment>)

internal fun SyncNoteRequest.typeKey() = TypeKey(type, subType)

internal fun SyncNoteRequest.asNoteAndAmendments(typeSupplier: (String, String) -> SubType) =
  asNote(typeSupplier).let { note ->
    note.legacyId = this.legacyId
    note.createdAt = createdDateTime
    note.createdBy = createdByUsername
    NoteAndAmendments(note, amendments.map { it.asAmendment(note) })
  }

private fun SyncAmendmentRequest.asAmendment(note: Note) = Amendment(
  note,
  authorUsername,
  authorName,
  authorUserId,
  text,
  newUuid(),
).apply { this.createdAt = this@asAmendment.createdDateTime }

internal fun SyncNoteRequest.asNoteWithAmendments(typeSupplier: (String, String) -> SubType) =
  asNote(typeSupplier).also { note -> amendments.forEach { note.addAmendment(it) } }

internal fun SyncNoteRequest.asNote(typeSupplier: (String, String) -> SubType) = Note(
  personIdentifier,
  typeSupplier(type, subType),
  occurrenceDateTime,
  locationId,
  authorUsername,
  authorUserId,
  authorName,
  text,
  systemGenerated,
).apply {
  this.legacyId = this@asNote.legacyId
  this.createdAt = this@asNote.createdDateTime
  this.createdBy = this@asNote.createdByUsername
}
