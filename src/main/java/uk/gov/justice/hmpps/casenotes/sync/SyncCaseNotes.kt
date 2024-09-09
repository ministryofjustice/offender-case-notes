package uk.gov.justice.hmpps.casenotes.sync

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.AmendmentRepository
import uk.gov.justice.hmpps.casenotes.domain.IdGenerator.newUuid
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.TypeKey
import uk.gov.justice.hmpps.casenotes.domain.TypeLookup
import java.util.TreeSet
import java.util.UUID

@Transactional
@Service
class SyncCaseNotes(
  private val typeRepository: SubTypeRepository,
  private val noteRepository: NoteRepository,
  private val amendmentRepository: AmendmentRepository,
) {
  fun caseNotes(caseNotes: List<SyncCaseNoteRequest>): List<SyncResult> {
    val types = getTypesForSync(caseNotes.map { it.typeKey() }.toSet())
    val (persist, _) = caseNotes.partition { it.id == null }
    val new = create(persist.map { it.asNoteAndAmendments { t, st -> requireNotNull(types[TypeKey(t, st)]) } })
    return new.map { SyncResult(it.id, it.legacyId) }
  }

  private fun SyncCaseNoteRequest.typeKey() = TypeKey(type, subType)

  private fun getTypesForSync(keys: Set<TypeKey>): Map<TypeKey, SubType> {
    val types = typeRepository.findByKeyIn(keys).associateBy { it.key }
    val missing = keys.subtract(types.keys)
    check(missing.isEmpty()) {
      "Case note types missing: ${missing.exceptionMessage()}"
    }
    val nonSyncToNomisTypes = types.values.filter { !it.syncToNomis }
    check(nonSyncToNomisTypes.isEmpty()) {
      "Case note types are not sync to nomis types: ${nonSyncToNomisTypes.exceptionMessage()}"
    }
    return types
  }

  private fun create(new: List<NoteAndAmendments>): List<Note> {
    val notes = noteRepository.saveAll(new.map { it.note })
    amendmentRepository.saveAll(new.flatMap { it.amendments })
    return notes
  }
}

private fun <T : TypeLookup> Collection<T>.exceptionMessage() =
  sortedBy { it.parentCode }
    .groupBy { it.parentCode }
    .map { e ->
      "${e.key}:${
        e.value.sortedBy { it.code }.joinToString(prefix = "[", postfix = "]", separator = ", ") { it.code }
      }"
    }
    .joinToString(separator = ", ", prefix = "{ ", postfix = " }")

private fun SyncCaseNoteRequest.asNoteAndAmendments(typeSupplier: (String, String) -> SubType) = Note(
  personIdentifier,
  typeSupplier(type, subType),
  occurrenceDateTime,
  locationId,
  authorUsername,
  authorUserId,
  authorName,
  text,
  systemGenerated,
  TreeSet(),
).let { note ->
  note.legacyId = this.legacyId
  note.createDateTime = createdDateTime
  note.createUserId = createdByUsername
  NoteAndAmendments(note, amendments.map { it.asAmendment(note) })
}

private fun SyncAmendmentRequest.asAmendment(note: Note) = Amendment(
  note,
  authorUsername,
  authorName,
  authorUserId,
  text,
  newUuid(),
).apply {
  this.createDateTime = this@asAmendment.createdDateTime
  this.createUserId = createdByUsername
}

private data class NoteAndAmendments(val note: Note, val amendments: List<Amendment>)

data class SyncResult(val id: UUID, val legacyId: Long)
