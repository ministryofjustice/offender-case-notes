package uk.gov.justice.hmpps.casenotes.utils

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.sync.SyncAmendmentRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncNoteRequest
import java.time.temporal.ChronoUnit.SECONDS

fun CaseNote.verifyAgainst(note: Note) {
  assertThat(type).isEqualTo(note.type.parent.code)
  assertThat(typeDescription).isEqualTo(note.type.parent.description)
  assertThat(subType).isEqualTo(note.type.code)
  assertThat(subType).isEqualTo(note.type.code)
  assertThat(text).isEqualTo(note.text)
  assertThat(authorName).isEqualTo(note.authorName)
  assertThat(authorUserId).isEqualTo(note.authorUserId)
  assertThat(systemGenerated).isEqualTo(note.systemGenerated)
  assertThat(legacyId).isEqualTo(note.legacyId)
}

fun CaseNoteAmendment.verifyAgainst(amendment: Amendment) {
  assertThat(additionalNoteText).isEqualTo(amendment.text)
  assertThat(authorName).isEqualTo(amendment.authorName)
  assertThat(authorUserId).isEqualTo(amendment.authorUserId)
}

fun Note.verifyAgainst(request: SyncNoteRequest) {
  assertThat(prisonNumber).isEqualTo(request.personIdentifier)
  assertThat(type.parent.code).isEqualTo(request.type)
  assertThat(type.code).isEqualTo(request.subType)
  assertThat(text).isEqualTo(request.text)
  assertThat(occurredAt.truncatedTo(SECONDS)).isEqualTo(request.occurrenceDateTime.truncatedTo(SECONDS))
  assertThat(createDateTime.truncatedTo(SECONDS)).isEqualTo(request.createdDateTime.truncatedTo(SECONDS))
  assertThat(authorName).isEqualTo(request.authorName)
  assertThat(authorUsername).isEqualTo(request.authorUsername)
  assertThat(authorUserId).isEqualTo(request.authorUserId)
  assertThat(legacyId).isEqualTo(request.legacyId)
  assertThat(createUserId).isEqualTo(request.createdByUsername)
}

fun Amendment.verifyAgainst(request: SyncAmendmentRequest) {
  assertThat(text).isEqualTo(request.text)
  assertThat(createDateTime.truncatedTo(SECONDS)).isEqualTo(request.createdDateTime.truncatedTo(SECONDS))
  assertThat(authorName).isEqualTo(request.authorName)
  assertThat(authorUsername).isEqualTo(request.authorUsername)
  assertThat(authorUserId).isEqualTo(request.authorUserId)
}
