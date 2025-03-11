package uk.gov.justice.hmpps.casenotes.sync

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
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
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
) {

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
        SyncOverrides.of(existing?.id, existing?.legacyId),
        typeRepository::getByTypeCodeAndCode,
      ),
    )

    eventPublisher.publishEvent(saved.createEvent(existing?.let { UPDATED } ?: CREATED))

    return SyncResult(
      saved.id,
      request.legacyId,
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
