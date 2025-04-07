package uk.gov.justice.hmpps.casenotes.domain.audit

import jakarta.persistence.PreRemove
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.DeletionCause
import uk.gov.justice.hmpps.casenotes.domain.NoteState
import uk.gov.justice.hmpps.casenotes.domain.System

@Component
class DeletedEntityListener {

  private lateinit var deletedRepository: DeletedCaseNoteRepository

  @Autowired
  fun setDeletedRepository(@Lazy deletedCaseNoteRepository: DeletedCaseNoteRepository) {
    deletedRepository = deletedCaseNoteRepository
  }

  @PreRemove
  fun preRemove(noteState: NoteState) {
    val context = CaseNoteRequestContext.get()
    deletedRepository.save(
      DeletedCaseNote(
        noteState.personIdentifier,
        noteState.getId(),
        noteState.legacyId,
        DeletedDetail(noteState),
        context.requestAt,
        context.username,
        when (context.source) {
          Source.DPS -> System.DPS
          Source.NOMIS -> System.NOMIS
        },
        causeOfDelete(),
        context.deletionReason,
      ),
    )
  }

  private fun causeOfDelete() = when (getRequestMethod()) {
    null -> DeletionCause.MERGE
    HttpMethod.DELETE -> DeletionCause.DELETE
    else -> {
      if (isMoveRequest()) {
        DeletionCause.MOVE
      } else {
        DeletionCause.UPDATE
      }
    }
  }

  private fun getRequestMethod(): HttpMethod? = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?)?.request?.let { HttpMethod.valueOf(it.method) }

  private fun isMoveRequest(): Boolean = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?)?.request?.requestURL
    ?.contains("/move/case-notes") == true
}
