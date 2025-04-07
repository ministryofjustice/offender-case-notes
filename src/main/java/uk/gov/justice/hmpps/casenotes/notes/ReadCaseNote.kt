package uk.gov.justice.hmpps.casenotes.notes

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest.of
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.by
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_ADMIN
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_SYNC
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.TypeKey
import uk.gov.justice.hmpps.casenotes.domain.matchesAuthorUsername
import uk.gov.justice.hmpps.casenotes.domain.matchesLocationId
import uk.gov.justice.hmpps.casenotes.domain.matchesOnType
import uk.gov.justice.hmpps.casenotes.domain.matchesPersonIdentifier
import uk.gov.justice.hmpps.casenotes.domain.occurredAfter
import uk.gov.justice.hmpps.casenotes.domain.occurredBefore
import uk.gov.justice.hmpps.casenotes.legacy.service.EntityNotFoundException
import uk.gov.justice.hmpps.casenotes.notes.NoteUsageRequest.DateType.CREATED_AT
import uk.gov.justice.hmpps.casenotes.notes.NoteUsageRequest.DateType.OCCURRED_AT
import java.time.temporal.ChronoUnit
import java.util.UUID.fromString

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_READ', '$ROLE_CASE_NOTES_WRITE', '$ROLE_CASE_NOTES_ADMIN', '$ROLE_CASE_NOTES_SYNC')")
class ReadCaseNote(
  private val noteRepository: NoteRepository,
) {
  fun caseNotes(personIdentifier: String, filter: CaseNoteFilter, pageable: Pageable): Page<CaseNote> {
    val page = noteRepository.findAll(filter.asSpecification(personIdentifier), pageable.forSpecification())
    val records = noteRepository.findAllByIdIn(page.content.map { it.id }).associateBy { it.id }
    return page.map { records[it.id]!!.toModel() }
  }

  fun caseNote(personIdentifier: String, caseNoteId: String): CaseNote {
    val caseNote = when (val legacyId = caseNoteId.asLegacyId()) {
      null -> noteRepository.findByIdAndPersonIdentifier(fromString(caseNoteId), personIdentifier)
      else -> noteRepository.findByLegacyIdAndPersonIdentifier(legacyId, personIdentifier)
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
    val (typeCodes, typeKeys) = request.allTypeKeys()
    val usage = when (request.dateType) {
      OCCURRED_AT -> noteRepository.findUsageByPersonIdentifierOccurredAt(
        request.personIdentifiers.toSet(),
        typeCodes,
        typeKeys,
        request.from,
        request.to,
        request.authorIds.takeUnless { it.isEmpty() },
        request.prisonCode,
      )

      CREATED_AT -> noteRepository.findUsageByPersonIdentifierCreatedAt(
        request.personIdentifiers.toSet(),
        typeCodes,
        typeKeys,
        request.from,
        request.to,
        request.authorIds.takeUnless { it.isEmpty() },
        request.prisonCode,
      )
    }
    return usage.map {
      UsageByPersonIdentifierResponse(
        it.key,
        it.type,
        it.subType,
        it.count,
        LatestNote(it.latestAt.truncatedTo(ChronoUnit.SECONDS)),
      )
    }.groupBy { it.personIdentifier }
  }

  fun findByAuthorId(request: UsageByAuthorIdRequest): Map<String, List<UsageByAuthorIdResponse>> {
    val (typeCodes, typeKeys) = request.allTypeKeys()
    val usage = when (request.dateType) {
      OCCURRED_AT -> noteRepository.findUsageByAuthorIdOccurredAt(
        request.authorIds.toSet(),
        typeCodes,
        typeKeys,
        request.from,
        request.to,
        request.prisonCode,
      )

      CREATED_AT -> noteRepository.findUsageByAuthorIdCreatedAt(
        request.authorIds.toSet(),
        typeCodes,
        typeKeys,
        request.from,
        request.to,
        request.prisonCode,
      )
    }
    return usage.map {
      UsageByAuthorIdResponse(
        it.key,
        it.type,
        it.subType,
        it.count,
        LatestNote(it.latestAt.truncatedTo(ChronoUnit.SECONDS)),
      )
    }.groupBy { it.authorId }
  }

  fun NoteUsageRequest.allTypeKeys(): Pair<Set<String>, Set<TypeKey>> {
    val providedSubTypes = typeSubTypes.flatMap { r -> r.subTypes.map { TypeKey(r.type, it) } }.toSet()
    val typeCodes = typeSubTypes.asSequence().filter { it.subTypes.isEmpty() }.map { it.type }.toSet()
    return Pair(typeCodes, providedSubTypes)
  }
}

private fun CaseNoteFilter.asSpecification(personIdentifier: String) = listOfNotNull(
  matchesPersonIdentifier(personIdentifier),
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

private fun SearchNotesRequest.asSpecification(personIdentifier: String) = listOfNotNull(
  matchesPersonIdentifier(personIdentifier),
  matchesOnType(includeSensitive, typeSubTypes.asMap()),
  occurredFrom?.let { occurredAfter(it) },
  occurredTo?.let { occurredBefore(it) },
).reduce { spec, current -> spec.and(current) }
