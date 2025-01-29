package uk.gov.justice.hmpps.casenotes.sync

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.AmendmentRepository
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.TypeKey
import uk.gov.justice.hmpps.casenotes.domain.TypeLookup
import uk.gov.justice.hmpps.casenotes.domain.createdBetween
import uk.gov.justice.hmpps.casenotes.domain.getByTypeCodeAndCode
import uk.gov.justice.hmpps.casenotes.domain.idIn
import uk.gov.justice.hmpps.casenotes.domain.saveAndRefresh
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Companion.createEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.CREATED
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.DELETED
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.MOVED
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.UPDATED
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.toModel
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Transactional
@Service
class SyncCaseNotes(
  private val typeRepository: SubTypeRepository,
  private val noteRepository: NoteRepository,
  private val amendmentRepository: AmendmentRepository,
  private val eventPublisher: ApplicationEventPublisher,
  private val telemetryClient: TelemetryClient,
) {
  fun migrationRequest(personIdentifier: String, toMigrate: List<MigrateCaseNoteRequest>): List<MigrationResult> {
    val existing = noteRepository.findNomisCaseNotesByPersonIdentifier(personIdentifier)
    val existingLegacyIds = existing.map(Note::legacyId).toSet()
    val toMigrateLegacyMap = toMigrate.associateBy(MigrateCaseNoteRequest::legacyId)

    val deleted = existing.filter { it.legacyId !in toMigrateLegacyMap.keys }.delete()
    val created = toMigrate.filter { it.legacyId !in existingLegacyIds }.mapToEntities(personIdentifier).create()
    val updated = existing.filter { it.legacyId in toMigrateLegacyMap.keys }
      .map {
        val dates = requireNotNull(toMigrateLegacyMap[it.legacyId])
        it.migrateDates(dates.occurrenceDateTime, dates.createdDateTime)
      }

    telemetryClient.trackEvent(
      "Migration Request",
      mapOf(
        "personIdentifier" to personIdentifier,
        "createdCaseNotes" to created.size.toString(),
        "createdAmendments" to created.flatMap { it.amendments() }.size.toString(),
        "deletedCaseNotes" to deleted.size.toString(),
        "deletedAmendments" to deleted.flatMap { it.amendments() }.size.toString(),
        "updatedCaseNotes" to updated.size.toString(),
      ),
      mapOf(),
    )
    return (created + updated).map { MigrationResult(it.id, it.legacyId) }
  }

  private fun List<Note>.delete(): List<Note> {
    val amendments = flatMap(Note::amendments)
    if (amendments.isNotEmpty()) {
      amendmentRepository.deleteByIdIn(amendments.map(Amendment::getId))
    }

    if (isNotEmpty()) {
      noteRepository.deleteByIdIn(map(Note::getId))
      forEach(noteRepository::detach)
    }
    return this
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
    return map { it.asNoteAndAmendments(personIdentifier, null) { t, st -> requireNotNull(types[TypeKey(t, st)]) } }
  }

  private fun List<NoteAndAmendments>.create(): List<Note> {
    val notes = noteRepository.saveAll(map { it.note })
    amendmentRepository.saveAll(flatMap { it.amendments })
    return notes
  }

  fun syncNote(request: SyncCaseNoteRequest): SyncResult {
    val existing = when (request.id) {
      null -> noteRepository.findByLegacyId(request.legacyId)
      else -> noteRepository.findByIdOrNull(request.id)
    }

    val amended: Note? = existing?.let {
      check(it.personIdentifier == request.personIdentifier) {
        "Case note belongs to another prisoner or prisoner records have been merged"
      }

      if (request updates existing) {
        noteRepository.delete(it)
        noteRepository.flush()
        null
      } else {
        existing amendWith request
      }
    }

    val saved = amended ?: noteRepository.saveAndRefresh(
      request.asNoteWithAmendments(
        request.personIdentifier,
        SyncOverrides.of(existing?.id),
        typeRepository::getByTypeCodeAndCode,
      ),
    )

    eventPublisher.publishEvent(saved.createEvent(existing?.let { UPDATED } ?: CREATED))

    return SyncResult(
      saved.id,
      saved.legacyId,
      if (existing == null) SyncResult.Action.CREATED else SyncResult.Action.UPDATED,
    )
  }

  fun deleteCaseNote(id: UUID) {
    noteRepository.findByIdOrNull(id)?.also {
      noteRepository.delete(it)
      eventPublisher.publishEvent(it.createEvent(DELETED))
    }
  }

  fun moveCaseNotes(request: MoveCaseNotesRequest) {
    val from = noteRepository.findAllByPersonIdentifierAndIdIn(request.fromPersonIdentifier, request.caseNoteIds)
    noteRepository.deleteAll(from)
    val (to, events) = from.map {
      val new = it.merge(request.toPersonIdentifier)
      val event = new.createEvent(MOVED, it.personIdentifier)
      new to event
    }.unzip()
    noteRepository.saveAll(to)
    amendmentRepository.saveAll(to.flatMap(Note::mergedAmendments))
    events.forEach(eventPublisher::publishEvent)
  }

  @Transactional(readOnly = true)
  fun getCaseNotes(personIdentifier: String): List<CaseNote> = noteRepository.findAllByPersonIdentifierAndSubTypeSyncToNomis(personIdentifier, true).map(Note::toModel)

  @Transactional(readOnly = true)
  fun resendEvents(request: ResendPersonCaseNoteEvents) {
    noteRepository.findAll(request.asSpecification())
      .map { it.createEvent(if (it.amendments().isEmpty()) CREATED else UPDATED, sourceOverride = Source.DPS) }
      .forEach(eventPublisher::publishEvent)
  }
}

private fun ResendPersonCaseNoteEvents.asSpecification(): Specification<Note> = listOfNotNull(
  if (uuids.isNotEmpty()) idIn(uuids) else null,
  createdBetween?.let { createdBetween(createdBetween.from, createdBetween.to, createdBetween.includeSyncToNomis) },
).reduce { spec, current -> spec.and(current) }

private fun <T : TypeLookup> Collection<T>.exceptionMessage() = sortedBy { it.typeCode }
  .groupBy { it.typeCode }
  .map { e ->
    "${e.key}:${
      e.value.sortedBy { it.code }.joinToString(prefix = "[", postfix = "]", separator = ", ") { it.code }
    }"
  }
  .joinToString(separator = ", ", prefix = "{ ", postfix = " }")

private infix fun SyncCaseNoteRequest.updates(note: Note): Boolean {
  val typeChanged = subType != note.subType.code || type != note.subType.typeCode
  val noteChanged = text != note.text
  val dateChanged = occurrenceDateTime != note.occurredAt
  return typeChanged || noteChanged || dateChanged || amendments.any { it updates note }
}

private infix fun SyncCaseNoteAmendmentRequest.updates(note: Note): Boolean = note.findAmendment(this)?.let { text != it.text } ?: false

private fun Note.findAmendment(request: SyncCaseNoteAmendmentRequest): Amendment? = amendments().singleOrNull { it.authorUsername == request.author.username && it.createdAt.isSameSecond(request.createdDateTime) }

private fun LocalDateTime.isSameSecond(other: LocalDateTime): Boolean = truncatedTo(ChronoUnit.SECONDS) == other.truncatedTo(ChronoUnit.SECONDS)

private infix fun Note.amendWith(request: SyncCaseNoteRequest) = apply {
  request.amendments.filter { findAmendment(it) == null }.forEach(::addAmendment)
}
