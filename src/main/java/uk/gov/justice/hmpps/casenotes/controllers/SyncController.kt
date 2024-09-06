package uk.gov.justice.hmpps.casenotes.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_SYNC
import uk.gov.justice.hmpps.casenotes.sync.SyncCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncCaseNotes
import uk.gov.justice.hmpps.casenotes.sync.SyncResult

@Tag(name = "Sync Case Notes")
@RestController
@RequestMapping("sync/case-notes")
class SyncController(private val sync: SyncCaseNotes) {
  @PutMapping
  @PreAuthorize("hasRole('$ROLE_CASE_NOTES_SYNC')")
  fun syncCaseNotes(@Valid @RequestBody caseNotes: List<SyncCaseNoteRequest>): List<SyncResult> = sync.caseNotes(caseNotes)
}
