package uk.gov.justice.hmpps.casenotes.sync

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.AmendmentRepository
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.TypeKey
import uk.gov.justice.hmpps.casenotes.domain.TypeLookup
import uk.gov.justice.hmpps.casenotes.domain.getByParentCodeAndCode
import uk.gov.justice.hmpps.casenotes.domain.saveAndRefresh
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Transactional
@Service
class SyncCaseNotes(
  private val typeRepository: SubTypeRepository,
  private val noteRepository: NoteRepository,
  private val amendmentRepository: AmendmentRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun migrateNotes(toMigrate: List<MigrateCaseNoteRequest>): List<MigrationResult> {
    val types = getTypesForSync(toMigrate.map { it.typeKey() }.toSet())
    val existingIds = noteRepository.findMigratedIds(toMigrate.map { it.legacyId })
    val existingLegacyIds = existingIds.map { it.legacyId }.toSet()
    val new = toMigrate.filter { it.legacyId !in existingLegacyIds }
      .map { it.asNoteAndAmendments { t, st -> requireNotNull(types[TypeKey(t, st)]) } }
    return create(new).map { MigrationResult(it.id, it.legacyId) } + existingIds
  }

  fun syncNote(request: SyncCaseNoteRequest): SyncResult {
    val existing = when (request.id) {
      null -> noteRepository.findByLegacyId(request.legacyId)
      else -> noteRepository.findByIdOrNull(request.id)
    }

    existing?.also {
      check(it.personIdentifier == request.personIdentifier) { "Case note belongs to another prisoner or prisoner records have been merged" }
    }

    val saved = existing?.sync(request)
      ?: noteRepository.saveAndRefresh(request.asNoteWithAmendments(typeRepository::getByParentCodeAndCode))

    return SyncResult(
      saved.id,
      saved.legacyId,
      if (existing == null) SyncResult.Action.CREATED else SyncResult.Action.UPDATED,
    )
  }

  fun deleteCaseNote(id: UUID) = noteRepository.deleteById(id).also {
    telemetryClient.trackEvent("CaseNoteDeletedViaSync", mapOf("id" to it.toString()), mapOf())
  }

  private fun Note.sync(request: SyncNoteRequest): Note? =
    if (!type.matches(request.type, request.subType) || text != request.text) {
      noteRepository.delete(this)
      noteRepository.flush()
      null
    } else {
      request.amendments.forEach {
        matchAmendment(it) ?: addAmendment(it)
      }
      this
    }

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

private fun SubType.matches(parentCode: String, code: String): Boolean {
  return this.code == code && this.parentCode == parentCode
}

private fun Note.matchAmendment(request: SyncAmendmentRequest): Amendment? {
  val matching = amendments().filter {
    request.authorUsername == it.authorUsername && request.createdDateTime.sameSecond(it.createdAt)
  }
  return when (matching.size) {
    1 -> matching.single()
    else -> null
  }
}

private fun LocalDateTime.sameSecond(other: LocalDateTime): Boolean =
  truncatedTo(ChronoUnit.SECONDS) == other.truncatedTo(ChronoUnit.SECONDS)
