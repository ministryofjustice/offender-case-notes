package uk.gov.justice.hmpps.casenotes.utils

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.audit.DeletedDetail
import uk.gov.justice.hmpps.casenotes.events.CaseNoteInformation
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.sync.SyncAmendmentRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncNoteRequest
import java.time.temporal.ChronoUnit.SECONDS

fun CaseNote.verifyAgainst(note: Note) {
  assertThat(type).isEqualTo(note.subType.type.code)
  assertThat(typeDescription).isEqualTo(note.subType.type.description)
  assertThat(subType).isEqualTo(note.subType.code)
  assertThat(subType).isEqualTo(note.subType.code)
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
  assertThat(subType.type.code).isEqualTo(request.type)
  assertThat(subType.code).isEqualTo(request.subType)
  assertThat(text).isEqualTo(request.text)
  assertThat(occurredAt.truncatedTo(SECONDS)).isEqualTo(request.occurrenceDateTime.truncatedTo(SECONDS))
  assertThat(createdAt.truncatedTo(SECONDS)).isEqualTo(request.createdDateTime.truncatedTo(SECONDS))
  assertThat(authorName).isEqualTo(request.author.fullName())
  assertThat(authorUsername).isEqualTo(request.author.username)
  assertThat(authorUserId).isEqualTo(request.author.userId)
  assertThat(legacyId).isEqualTo(request.legacyId)
  assertThat(createdBy).isEqualTo(request.createdByUsername)
}

fun Amendment.verifyAgainst(request: SyncAmendmentRequest) {
  assertThat(text).isEqualTo(request.text)
  assertThat(createdAt.truncatedTo(SECONDS)).isEqualTo(request.createdDateTime.truncatedTo(SECONDS))
  assertThat(authorName).isEqualTo(request.author.fullName())
  assertThat(authorUsername).isEqualTo(request.author.username)
  assertThat(authorUserId).isEqualTo(request.author.userId)
}

fun DeletedDetail.verifyAgainst(note: Note) {
  assertThat(personIdentifier).isEqualTo(note.personIdentifier)
  assertThat(subTypeId).isEqualTo(note.subType.id)
  assertThat(text).isEqualTo(note.text)
  assertThat(occurredAt.truncatedTo(SECONDS)).isEqualTo(note.occurredAt.truncatedTo(SECONDS))
  assertThat(createdAt.truncatedTo(SECONDS)).isEqualTo(note.createdAt.truncatedTo(SECONDS))
  assertThat(authorName).isEqualTo(note.authorName)
  assertThat(authorUsername).isEqualTo(note.authorUsername)
  assertThat(authorUserId).isEqualTo(note.authorUserId)
  assertThat(legacyId).isEqualTo(note.legacyId)
  assertThat(createdBy).isEqualTo(note.createdBy)
}

fun DomainEvent<CaseNoteInformation>.verifyAgainst(eventType: PersonCaseNoteEvent.Type, source: Source, saved: Note) {
  assertThat(this.eventType).isEqualTo("person.case-note.${eventType.name.lowercase()}")
  assertThat(detailUrl).isEqualTo("http://localhost:8080/case-notes/${saved.personIdentifier}/${saved.id}")
  assertThat(personReference.findNomsNumber()).isEqualTo(saved.personIdentifier)
  with(additionalInformation) {
    assertThat(id).isEqualTo(saved.id)
    assertThat(type).isEqualTo(saved.subType.type.code)
    assertThat(subType).isEqualTo(saved.subType.code)
    assertThat(legacyId).isEqualTo(saved.legacyId)
    assertThat(this.source).isEqualTo(source)
    assertThat(syncToNomis).isEqualTo(saved.subType.syncToNomis)
    assertThat(systemGenerated).isEqualTo(saved.systemGenerated)
  }
}
