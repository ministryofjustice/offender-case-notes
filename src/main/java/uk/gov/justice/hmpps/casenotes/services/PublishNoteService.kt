package uk.gov.justice.hmpps.casenotes.services

import org.springframework.scheduling.annotation.Async
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository
import java.time.LocalDateTime
import java.util.stream.Collectors

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Service
@Transactional(readOnly = true)
@Validated
open class PublishNoteService(private val repository: OffenderCaseNoteRepository,
                              private val caseNoteEventPusher: CaseNoteEventPusher) {

  @PreAuthorize("hasAnyRole('PUBLISH_SENSITIVE_CASE_NOTES')")
  open fun findCaseNotes(fromDateTime: LocalDateTime = LocalDateTime.MIN, toDateTime: LocalDateTime): List<CaseNote> {
    return repository.findByModifyDateTimeBetweenOrderByModifyDateTime(fromDateTime, toDateTime).stream().map {
      CaseNote.builder()
          .caseNoteId(it.id.toString())
          .offenderIdentifier(it.offenderIdentifier)
          .type(it.sensitiveCaseNoteType.parentType.type)
          .subType(it.sensitiveCaseNoteType.type)
          .creationDateTime(it.createDateTime)
          .locationId(it.locationId)
          .build()
    }.collect(Collectors.toList<CaseNote>())
  }

  @Async
  open fun pushCaseNotes(caseNotes: List<CaseNote>) {
    caseNotes.forEach {
      caseNoteEventPusher.sendEvent(it)
    }
  }
}
