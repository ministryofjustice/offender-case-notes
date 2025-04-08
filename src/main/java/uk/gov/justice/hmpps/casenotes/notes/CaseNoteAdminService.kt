package uk.gov.justice.hmpps.casenotes.notes

import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.domain.SubTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.TypeKey
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Companion.createEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.DELETED
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.UPDATED
import uk.gov.justice.hmpps.casenotes.legacy.service.EntityNotFoundException
import java.util.UUID

@Transactional
@Service
class CaseNoteAdminService(
  private val noteRepository: NoteRepository,
  private val subTypeRepository: SubTypeRepository,
  private val eventPublisher: ApplicationEventPublisher,
) {

  fun replaceCaseNote(id: UUID, request: ReplaceNoteRequest): CaseNote {
    val existing = noteRepository.findByIdOrNull(id) ?: throw EntityNotFoundException.withId(id.toString())
    noteRepository.delete(existing)
    noteRepository.flush()
    return noteRepository.save(
      request.asNote(existing) { domain, code -> checkNotNull(subTypeRepository.findByKey(TypeKey(domain, code))) },
    ).also { eventPublisher.publishEvent(it.createEvent(UPDATED)) }.toModel()
  }

  fun deleteNote(id: UUID) {
    val existing = noteRepository.findByIdOrNull(id) ?: throw EntityNotFoundException.withId(id.toString())
    noteRepository.delete(existing)
    eventPublisher.publishEvent(existing.createEvent(DELETED))
  }

  fun ReplaceNoteRequest.asNote(
    existing: Note,
    typeSupplier: (String, String) -> SubType,
  ): Note = Note(
    existing.personIdentifier,
    typeSupplier(type, subType),
    occurrenceDateTime,
    existing.locationId,
    existing.authorUsername,
    existing.authorUserId,
    existing.authorName,
    text,
    existing.systemGenerated,
    existing.system,
    existing.id,
  ).apply {
    this.legacyId = existing.legacyId
    this.createdAt = existing.createdAt
    this.createdBy = existing.createdBy
    amendments.map {
      withAmendment(it) {
        existing.findAmendment(it) ?: throw EntityNotFoundException("No amendment for this case note with id $it")
      }
    }
  }
}
