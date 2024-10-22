package uk.gov.justice.hmpps.casenotes.merge

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.domain.AmendmentRepository
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.matchesOnType
import uk.gov.justice.hmpps.casenotes.domain.matchesPersonIdentifier
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.MergeInformation
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Companion.createEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type.MOVED

@Service
@Transactional
class MergeEventHandler(
  private val noteRepository: NoteRepository,
  private val amendmentRepository: AmendmentRepository,
  private val eventPublisher: ApplicationEventPublisher,
) {
  fun handle(event: DomainEvent<MergeInformation>) {
    val existingIds = noteRepository.findAll(
      matchesPersonIdentifier(event.additionalInformation.removedNomsNumber)
        .and(matchesOnType(true, mapOf())),
    ).map { it.id }
    val existing = noteRepository.findAllByIdIn(existingIds)
    val (toMerge, events) = existing.map {
      val merged = it.merge(event.additionalInformation.nomsNumber)
      merged to merged.createEvent(MOVED, it.personIdentifier)
    }.unzip()
    noteRepository.deleteAll(existing)
    noteRepository.saveAll(toMerge)
    amendmentRepository.saveAll(toMerge.flatMap(Note::mergedAmendments))
    events.forEach(eventPublisher::publishEvent)
  }
}
