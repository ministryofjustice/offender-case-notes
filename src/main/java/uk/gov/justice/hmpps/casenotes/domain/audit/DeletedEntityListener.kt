package uk.gov.justice.hmpps.casenotes.domain.audit

import jakarta.persistence.PreRemove
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.domain.DeletionCause
import uk.gov.justice.hmpps.casenotes.domain.NoteState
import uk.gov.justice.hmpps.casenotes.notes.DeletedCaseNote
import uk.gov.justice.hmpps.casenotes.notes.DeletedCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.notes.DeletedDetail

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
        context.source,
        causeOfDelete(),
      ),
    )
  }

  private fun causeOfDelete() = when (getRequestMethod()) {
    null -> DeletionCause.MERGE
    HttpMethod.DELETE -> DeletionCause.DELETE
    else -> DeletionCause.UPDATE
  }

  private fun getRequestMethod(): HttpMethod? =
    (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?)?.request?.let { HttpMethod.valueOf(it.method) }
}
