package uk.gov.justice.hmpps.casenotes.sync

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.hmpps.casenotes.domain.AmendmentRepository
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.TypeKey
import uk.gov.justice.hmpps.casenotes.domain.TypeLookup
import uk.gov.justice.hmpps.casenotes.domain.getByTypeCodeAndCode
import uk.gov.justice.hmpps.casenotes.domain.saveAndRefresh
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Companion.createEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.CREATED
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.DELETED
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.UPDATED
import java.util.UUID

@Transactional
@Service
class SyncCaseNotes(
  private val typeRepository: SubTypeRepository,
  private val noteRepository: NoteRepository,
  private val amendmentRepository: AmendmentRepository,
  private val eventPublisher: ApplicationEventPublisher,
  private val telemetryClient: TelemetryClient,
  private val transactionTemplate: TransactionTemplate,
) {
  @Transactional(propagation = Propagation.NEVER)
  fun migrateNotes(personIdentifier: String, toMigrate: List<MigrateCaseNoteRequest>): List<MigrationResult> {
    val (created, replaced) = try {
      transactionTemplate.execute {
        create(toMigrate.mapToEntities(personIdentifier)) to false
      }
    } catch (dive: DataIntegrityViolationException) {
      transactionTemplate.execute {
        amendmentRepository.deleteLegacyAmendments(personIdentifier)
        noteRepository.deleteLegacyCaseNotes(personIdentifier)
        create(toMigrate.mapToEntities(personIdentifier)) to true
      }
    }

    return created.map { MigrationResult(it.id, it.legacyId) }.also {
      telemetryClient.trackEvent(
        "CaseNotesMigrated",
        mapOf(
          "personIdentifier" to personIdentifier,
          "count" to toMigrate.count().toString(),
          "replaced" to replaced.toString(),
        ),
        mapOf(),
      )
    }
  }

  fun syncNote(request: SyncCaseNoteRequest): SyncResult {
    val existing = when (request.id) {
      null -> noteRepository.findByLegacyId(request.legacyId)
      else -> noteRepository.findByIdOrNull(request.id)
    }

    existing?.also {
      check(it.personIdentifier == request.personIdentifier) {
        "Case note belongs to another prisoner or prisoner records have been merged"
      }
      noteRepository.delete(it)
      noteRepository.flush()
    }

    val saved = noteRepository.saveAndRefresh(
      request.asNoteWithAmendments(
        request.personIdentifier,
        typeRepository::getByTypeCodeAndCode,
      ),
    )

    eventPublisher.publishEvent(saved.createEvent(existing?.let { UPDATED } ?: CREATED))

    return SyncResult(
      saved.id,
      saved.legacyId,
      if (existing == null) SyncResult.Action.CREATED else SyncResult.Action.UPDATED,
    ).also {
      telemetryClient.trackEvent(
        "CaseNoteSynced",
        saved.eventProperties() + listOfNotNull(existing?.id?.let { "previousId" to it.toString() }),
        mapOf(),
      )
    }
  }

  fun deleteCaseNote(id: UUID) {
    noteRepository.findByIdOrNull(id)?.also {
      noteRepository.delete(it)
      eventPublisher.publishEvent(it.createEvent(DELETED))
      telemetryClient.trackEvent("CaseNoteDeletedViaSync", it.eventProperties(), mapOf())
    }
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

  private fun List<MigrateCaseNoteRequest>.mapToEntities(personIdentifier: String): List<NoteAndAmendments> {
    val types = getTypesForSync(map { it.typeKey() }.toSet())
    return map { it.asNoteAndAmendments(personIdentifier) { t, st -> requireNotNull(types[TypeKey(t, st)]) } }
  }

  private fun create(new: List<NoteAndAmendments>): List<Note> {
    val notes = noteRepository.saveAll(new.map { it.note })
    amendmentRepository.saveAll(new.flatMap { it.amendments })
    noteRepository.flush()
    return notes
  }
}

private fun <T : TypeLookup> Collection<T>.exceptionMessage() =
  sortedBy { it.typeCode }
    .groupBy { it.typeCode }
    .map { e ->
      "${e.key}:${
        e.value.sortedBy { it.code }.joinToString(prefix = "[", postfix = "]", separator = ", ") { it.code }
      }"
    }
    .joinToString(separator = ", ", prefix = "{ ", postfix = " }")

private fun Note.eventProperties(): Map<String, String> {
  return mapOf(
    "id" to id.toString(),
    "type" to subType.type.code,
    "subType" to subType.code,
    "personIdentifier" to personIdentifier,
  )
}
