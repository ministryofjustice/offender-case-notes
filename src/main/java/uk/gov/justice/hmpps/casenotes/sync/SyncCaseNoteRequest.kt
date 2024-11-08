package uk.gov.justice.hmpps.casenotes.sync

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.IdGenerator.newUuid
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.System
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

  @get:Schema(
    requiredMode = REQUIRED,
    example = "A1234BC",
    description = "The offender/prison/prisoner/noms number - used to identify the person in prison",
  )
  @get:Length(max = 12, message = "person identifier cannot be more than 12 characters")
  @get:NotBlank(message = "person identifier cannot be blank")
  val personIdentifier: String,
  override val locationId: String,
  override val type: String,
  override val subType: String,
  override val occurrenceDateTime: LocalDateTime,
  override val text: String,
  override val systemGenerated: Boolean,
  override val author: Author,
  override val createdDateTime: LocalDateTime,
  override val createdByUsername: String,
  override val amendments: Set<SyncCaseNoteAmendmentRequest>,
  override val system: System?,
) : SyncNoteRequest

data class SyncCaseNoteAmendmentRequest(
  override val text: String,
  override val author: Author,
  override val createdDateTime: LocalDateTime,
  override val system: System?,
) : SyncAmendmentRequest

internal data class NoteAndAmendments(val note: Note, val amendments: List<Amendment>)

internal fun SyncNoteRequest.typeKey() = TypeKey(type, subType)

internal data class SyncOverrides(
  val id: UUID?,
) {
  companion object {
    fun of(id: UUID?) = if (id == null) null else SyncOverrides(id)
  }
}

internal fun SyncNoteRequest.asNoteAndAmendments(
  personIdentifier: String,
  syncOverrides: SyncOverrides?,
  typeSupplier: (String, String) -> SubType,
) = asNote(personIdentifier, syncOverrides, typeSupplier).let { note ->
  note.legacyId = this.legacyId
  note.createdAt = createdDateTime
  note.createdBy = createdByUsername
  NoteAndAmendments(note, amendments.map { it.asAmendment(note) })
}

private fun SyncAmendmentRequest.asAmendment(note: Note) = Amendment(
  note,
  author.username,
  author.fullName(),
  author.userId,
  text,
  system ?: System.NOMIS,
  newUuid(),
).apply { this.createdAt = this@asAmendment.createdDateTime }

internal fun SyncNoteRequest.asNoteWithAmendments(
  personIdentifier: String,
  syncOverrides: SyncOverrides?,
  typeSupplier: (String, String) -> SubType,
) = asNote(personIdentifier, syncOverrides, typeSupplier).also { note -> amendments.forEach { note.addAmendment(it) } }

internal fun SyncNoteRequest.asNote(
  personIdentifier: String,
  overrides: SyncOverrides?,
  typeSupplier: (String, String) -> SubType,
) = Note(
  personIdentifier,
  typeSupplier(type, subType),
  occurrenceDateTime,
  locationId,
  author.username,
  author.userId,
  author.fullName(),
  text,
  systemGenerated,
  system ?: System.NOMIS,
  id = overrides?.id ?: newUuid(),
).apply {
  this.legacyId = this@asNote.legacyId
  this.createdAt = this@asNote.createdDateTime
  this.createdBy = this@asNote.createdByUsername
}
