package uk.gov.justice.hmpps.casenotes.notes

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.by
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_SYNC
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.matchesAuthorUsername
import uk.gov.justice.hmpps.casenotes.domain.matchesLocationId
import uk.gov.justice.hmpps.casenotes.domain.matchesOnType
import uk.gov.justice.hmpps.casenotes.domain.matchesPrisonNumber
import uk.gov.justice.hmpps.casenotes.domain.occurredAfter
import uk.gov.justice.hmpps.casenotes.domain.occurredBefore
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter
import uk.gov.justice.hmpps.casenotes.services.EntityNotFoundException
import java.util.UUID.fromString

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_READ', '$ROLE_CASE_NOTES_WRITE', '$ROLE_CASE_NOTES_SYNC')")
class ReadCaseNote(
  private val noteRepository: NoteRepository,
) {
  fun caseNotes(prisonNumber: String, filter: CaseNoteFilter, pageable: Pageable): Page<CaseNote> {
    val page = noteRepository.findAll(filter.asSpecification(prisonNumber), pageable.forSpecification())
    val records = noteRepository.findAllByIdIn(page.content.map { it.id }).associateBy { it.id }
    return page.map { records[it.id]!!.toModel() }
  }

  fun caseNote(prisonNumber: String, caseNoteId: String): CaseNote {
    val caseNote = when (val legacyId = caseNoteId.asLegacyId()) {
      null -> noteRepository.findByIdAndPersonIdentifier(fromString(caseNoteId), prisonNumber)
      else -> noteRepository.findByLegacyIdAndPersonIdentifier(legacyId, prisonNumber)
    } ?: throw EntityNotFoundException.withId(caseNoteId)
    return caseNote.toModel()
  }

  companion object {
    const val SOURCE = "OCNS"
  }
}

private fun CaseNoteFilter.asSpecification(prisonNumber: String) =
  listOfNotNull(
    matchesPrisonNumber(prisonNumber),
    matchesOnType(includeSensitive, getTypesAndSubTypes()),
    locationId?.let { matchesLocationId(it) },
    authorUsername?.let { matchesAuthorUsername(it) },
    startDate?.let { occurredAfter(it) },
    endDate?.let { occurredBefore(it) },
  ).reduce { spec, current -> spec.and(current) }

private fun Pageable.forSpecification(): Pageable {
  val occurredAtSort = sort.getOrderFor("occurrenceDateTime")?.direction?.let { by(it, Note.OCCURRED_AT) }
  val sort = occurredAtSort ?: sort.getOrderFor("creationDateTime")?.direction?.let { by(it, Note.CREATED_AT) }
  return PageRequest.of(pageNumber, pageSize, sort ?: by(Sort.Direction.DESC, Note.OCCURRED_AT))
}
