package uk.gov.justice.hmpps.casenotes.notes

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest.of
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.by
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_SYNC
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.authorUserIdIn
import uk.gov.justice.hmpps.casenotes.domain.matchesAuthorUsername
import uk.gov.justice.hmpps.casenotes.domain.matchesLocationId
import uk.gov.justice.hmpps.casenotes.domain.matchesOnType
import uk.gov.justice.hmpps.casenotes.domain.matchesPersonIdentifier
import uk.gov.justice.hmpps.casenotes.domain.occurredAfter
import uk.gov.justice.hmpps.casenotes.domain.occurredBefore
import uk.gov.justice.hmpps.casenotes.domain.personIdentifierIn
import uk.gov.justice.hmpps.casenotes.legacy.service.EntityNotFoundException
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

  fun findNotes(personIdentifier: String, request: SearchNotesRequest): SearchNotesResponse {
    val page = noteRepository.findAll(request.asSpecification(personIdentifier), request.pageable())
    val records = noteRepository.findAllByIdIn(page.content.map { it.id }).associateBy { it.id }
    val results = page.content.map { records[it.id]!!.toModel() }
    val hasCaseNotes = {
      val sensitiveValues = when (request.includeSensitive) {
        true -> setOf(true, false)
        false -> setOf(false)
      }
      noteRepository.existsByPersonIdentifierAndSubTypeSensitiveIn(personIdentifier, sensitiveValues)
    }
    return SearchNotesResponse(
      results,
      PageMeta(page.totalElements.toInt(), request.page, request.size),
      records.isNotEmpty() || hasCaseNotes(),
    )
  }

  fun findByPersonIdentifier(request: UsageByPersonIdentifierRequest): Map<String, List<UsageByPersonIdentifierResponse>> {
    val map = noteRepository.findAll(request.asSpecification())
      .groupBy { listOf(it.personIdentifier, it.subType.typeCode, it.subType.code) }.toMap()
    return map.entries.map {
      UsageByPersonIdentifierResponse(
        it.key[0],
        it.key[1],
        it.key[2],
        it.value.count(),
        it.value.latest(),
      )
    }.groupBy { it.personIdentifier }
  }

  fun findByAuthorId(request: UsageByAuthorIdRequest): Map<String, List<UsageByAuthorIdResponse>> {
    val map = noteRepository.findAll(request.asSpecification())
      .groupBy { listOf(it.authorUserId, it.subType.typeCode, it.subType.code) }.toMap()
    return map.entries.map {
      UsageByAuthorIdResponse(
        it.key[0],
        it.key[1],
        it.key[2],
        it.value.count(),
        it.value.latest(),
      )
    }.groupBy { it.authorId }
  }
}

private fun CaseNoteFilter.asSpecification(prisonNumber: String) =
  listOfNotNull(
    matchesPersonIdentifier(prisonNumber),
    matchesOnType(includeSensitive, getTypesAndSubTypes()),
    locationId?.let { matchesLocationId(it) },
    authorUsername?.let { matchesAuthorUsername(it) },
    startDate?.let { occurredAfter(it) },
    endDate?.let { occurredBefore(it) },
  ).reduce { spec, current -> spec.and(current) }

private fun Pageable.forSpecification(): Pageable {
  val occurredAtSort = sort.getOrderFor("occurrenceDateTime")?.direction?.let { by(it, Note.OCCURRED_AT) }
  val sort = occurredAtSort ?: sort.getOrderFor("creationDateTime")?.direction?.let { by(it, Note.CREATED_AT) }
  return of(pageNumber, pageSize, sort ?: by(Sort.Direction.DESC, Note.OCCURRED_AT))
}

private fun Set<TypeSubTypeRequest>.asMap() = associate { it.type to it.subTypes }

private fun SearchNotesRequest.asSpecification(personIdentifier: String) =
  listOfNotNull(
    matchesPersonIdentifier(personIdentifier),
    matchesOnType(includeSensitive, typeSubTypes.asMap()),
    occurredFrom?.let { occurredAfter(it) },
    occurredTo?.let { occurredBefore(it) },
  ).reduce { spec, current -> spec.and(current) }

private fun NoteUsageRequest.specifications() = listOfNotNull(
  matchesOnType(true, typeSubTypes.asMap()),
  occurredFrom?.let { occurredAfter(it) },
  occurredTo?.let { occurredBefore(it) },
)

private fun UsageByPersonIdentifierRequest.asSpecification(): Specification<Note> = buildList {
  addAll(specifications())
  add(personIdentifierIn(personIdentifiers))
  if (authorIds.isNotEmpty()) add(authorUserIdIn(authorIds))
}.reduce { spec, current -> spec.and(current) }

private fun UsageByAuthorIdRequest.asSpecification(): Specification<Note> =
  (specifications() + authorUserIdIn(authorIds)).reduce { spec, current -> spec.and(current) }

private fun List<Note>.latest() = maxBy { it.occurredAt }.let { LatestNote(it.id, it.occurredAt) }
