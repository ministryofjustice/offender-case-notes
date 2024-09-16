package uk.gov.justice.hmpps.casenotes.domain.audit

import jakarta.persistence.PreRemove
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.domain.NoteState

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
        noteState.id,
        noteState.legacyId,
        DeletedDetail(noteState),
        context.requestAt,
        context.username,
      ),
    )
  }
}
