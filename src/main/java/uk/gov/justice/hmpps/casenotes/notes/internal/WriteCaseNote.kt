package uk.gov.justice.hmpps.casenotes.notes.internal

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest

@Service
@Transactional
@PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_WRITE')")
class WriteCaseNote {
  fun note(prisonNumber: String, request: CreateCaseNoteRequest): CaseNote {
    TODO()
  }

  fun amendment(): CaseNoteAmendment {
    TODO()
  }
}
