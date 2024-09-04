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
@PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_READ', '$ROLE_CASE_NOTES_WRITE')")
class ReadCaseNote(
  private val noteRepository: NoteRepository,
) {
  fun caseNotes(prisonNumber: String, filter: CaseNoteFilter, pageable: Pageable): Page<CaseNote> =
    noteRepository.findAll(filter.asSpecification(prisonNumber), pageable.forSpecification()).map { it.toModel() }

  fun caseNote(prisonNumber: String, caseNoteId: String): CaseNote {
    val caseNote = when (val legacyId = caseNoteId.asLegacyId()) {
      null -> noteRepository.findByIdAndPrisonNumber(fromString(caseNoteId), prisonNumber)
      else -> noteRepository.findByLegacyIdAndPrisonNumber(legacyId, prisonNumber)
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
    matchesOnType(includeSensitive, getTypesAndSubTypes().map(String::asTypePair).asMap()),
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

private fun String.asTypePair(): Pair<String, String?> {
  val split = this.trim().replaceFirst(" ", "+").split("+")
  return when (split.size) {
    1 -> Pair(split[0], null)
    else -> Pair(split[0], split[1])
  }
}

private fun List<Pair<String, String?>>.asMap(): Map<String, Set<String>> =
  groupBy({ it.first }, { it.second }).mapValues { t -> t.value.mapNotNull { it }.toSet() }
