package uk.gov.justice.hmpps.casenotes.utils

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.notes.internal.Amendment
import uk.gov.justice.hmpps.casenotes.notes.internal.Note

fun CaseNote.verifyAgainst(note: Note) {
  assertThat(type).isEqualTo(note.type.category.code)
  assertThat(typeDescription).isEqualTo(note.type.category.description)
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
