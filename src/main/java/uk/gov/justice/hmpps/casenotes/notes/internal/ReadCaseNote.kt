package uk.gov.justice.hmpps.casenotes.notes.internal

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.by
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter
import uk.gov.justice.hmpps.casenotes.notes.internal.ReadCaseNote.Companion.SOURCE
import uk.gov.justice.hmpps.casenotes.services.EntityNotFoundException
import java.util.UUID.fromString

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_READ', '$ROLE_CASE_NOTES_WRITE')")
class ReadCaseNote(
  private val caseNoteRepository: CaseNoteRepository,
) {
  fun caseNotes(prisonNumber: String, filter: CaseNoteFilter, pageable: Pageable): Page<CaseNote> =
    caseNoteRepository.findAll(filter.asSpecification(prisonNumber), pageable.forSpecification()).map { it.toModel() }

  fun caseNote(prisonNumber: String, caseNoteId: String): CaseNote {
    val caseNote = when (val legacyId = caseNoteId.asLegacyId()) {
      null -> caseNoteRepository.findByIdAndPrisonNumber(fromString(caseNoteId), prisonNumber)
      else -> caseNoteRepository.findByLegacyIdAndPrisonNumber(legacyId, prisonNumber)
    } ?: throw EntityNotFoundException.withId(caseNoteId)
    return caseNote.toModel()
  }

  private fun String.asLegacyId() = toLongOrNull()

  companion object {
    const val SOURCE = "OCNS"
  }
}

private fun Note.toModel() = CaseNote(
  caseNoteId = id.toString(),
  offenderIdentifier = prisonNumber,
  type = type.category.code,
  typeDescription = type.category.description,
  subType = type.code,
  subTypeDescription = type.description,
  source = SOURCE,
  creationDateTime = createDateTime,
  occurrenceDateTime = occurredAt,
  authorName = authorName,
  authorUserId = authorUserId,
  text = text,
  locationId = locationId,
  eventId = requireNotNull(eventId),
  sensitive = type.sensitive,
  systemGenerated = systemGenerated,
  legacyId = legacyId,
  amendments = amendments().map { it.toModel() },
)

private fun Amendment.toModel() =
  CaseNoteAmendment(id!!, createDateTime, authorUsername, authorName, authorUserId, text)

private fun CaseNoteFilter.asSpecification(prisonNumber: String) =
  listOfNotNull(
    matchesPrisonNumber(prisonNumber),
    matchesOnType(includeSensitive),
    locationId?.let { matchesLocationId(it) },
    authorUsername?.let { matchesAuthorUsername(it) },
    startDate?.let { occurredAfter(it) },
    endDate?.let { occurredBefore(it) },
  ).reduce { spec, current -> spec.and(current) }

private fun Pageable.forSpecification(): Pageable {
  val occurredAtSort = sort.getOrderFor("occurrenceDateTime")?.direction?.let { by(it, Note.OCCURRED_AT) }
  val sort = occurredAtSort ?: sort.getOrderFor("creationDateTime")?.direction?.let { by(it, Note.CREATED_AT) }
  return PageRequest.of(pageNumber, pageSize, sort!!)
}
