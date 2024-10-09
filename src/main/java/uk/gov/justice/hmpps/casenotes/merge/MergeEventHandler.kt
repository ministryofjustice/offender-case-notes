package uk.gov.justice.hmpps.casenotes.merge

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.domain.AmendmentRepository
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.matchesOnType
import uk.gov.justice.hmpps.casenotes.domain.matchesPersonIdentifier
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.MergeInformation

@Service
@Transactional
class MergeEventHandler(
  private val noteRepository: NoteRepository,
  private val amendmentRepository: AmendmentRepository,
) {
  fun handle(event: DomainEvent<MergeInformation>) {
    val existingIds = noteRepository.findAll(
      matchesPersonIdentifier(event.additionalInformation.removedNomsNumber)
        .and(matchesOnType(true, mapOf())),
    ).map { it.id }
    val existing = noteRepository.findAllByIdIn(existingIds)
    val toMerge = existing.map { it.merge(event.additionalInformation.nomsNumber) }
    noteRepository.deleteAll(existing)
    noteRepository.saveAll(toMerge)
    amendmentRepository.saveAll(toMerge.flatMap(Note::mergedAmendments))
  }
}
