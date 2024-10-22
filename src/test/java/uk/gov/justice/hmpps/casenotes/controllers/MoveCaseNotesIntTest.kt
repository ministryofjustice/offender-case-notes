package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_SYNC
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.domain.DeletionCause
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.audit.DeletedCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.domain.matchesPersonIdentifier
import uk.gov.justice.hmpps.casenotes.sync.MoveCaseNotesRequest
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.util.UUID

class MoveCaseNotesIntTest : IntegrationTest() {
  @Autowired
  lateinit var deletedRepository: DeletedCaseNoteRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.put().uri(BASE_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    moveCaseNotes(moveCaseNotesRequest(), listOf(ROLE_CASE_NOTES_WRITE)).expectStatus().isForbidden
  }

  @Test
  fun `receiving a move request moves all to new person identifier`() {
    val oldNoms = personIdentifier()
    val cn1 = givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    val cn2 = givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(2)

    val newNoms = personIdentifier()
    moveCaseNotes(moveCaseNotesRequest(oldNoms, newNoms, setOf(cn1.id, cn2.id))).expectStatus().isOk

    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(0)
    assertThat(noteRepository.findAll(matchesPersonIdentifier(newNoms)).size).isEqualTo(2)

    verifyMoveAudit(cn1)
    verifyMoveAudit(cn2)

    verifyEvents(oldNoms, newNoms, cn1.id, cn2.id)
  }

  @Test
  fun `receiving a move request merges with existing case notes`() {
    val oldNoms = personIdentifier()
    val cn1 = givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    val cn2 = givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(2)

    val newNoms = personIdentifier()
    givenCaseNote(generateCaseNote(newNoms).withAmendment().withAmendment())
    assertThat(noteRepository.findAll(matchesPersonIdentifier(newNoms)).size).isEqualTo(1)

    moveCaseNotes(moveCaseNotesRequest(oldNoms, newNoms, setOf(cn1.id, cn2.id))).expectStatus().isOk

    assertThat(noteRepository.findAll(matchesPersonIdentifier(newNoms)).size).isEqualTo(3)
    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(0)

    verifyMoveAudit(cn1)
    verifyMoveAudit(cn2)

    verifyEvents(oldNoms, newNoms, cn1.id, cn2.id)
  }

  @Test
  fun `receiving a move request without case notes does not cause a failure`() {
    moveCaseNotes(moveCaseNotesRequest(caseNoteIds = setOf(UUID.randomUUID(), UUID.randomUUID()))).expectStatus().isOk
  }

  private fun moveCaseNotesRequest(
    from: String = personIdentifier(),
    to: String = personIdentifier(),
    caseNoteIds: Set<UUID> = setOf(),
  ) = MoveCaseNotesRequest(from, to, caseNoteIds)

  private fun moveCaseNotes(
    request: MoveCaseNotesRequest,
    roles: List<String> = listOf(ROLE_CASE_NOTES_SYNC),
  ) = webTestClient.put().uri(BASE_URL)
    .headers(addBearerAuthorisation(USERNAME, roles))
    .bodyValue(request).exchange()

  private fun verifyMoveAudit(note: Note) {
    val deleted = deletedRepository.findByCaseNoteId(note.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(DeletionCause.MOVE)
    assertThat(deleted.deletedBy).isEqualTo("SYS")
    deleted.caseNote.verifyAgainst(note)
  }

  private fun verifyEvents(oldNoms: String, newNoms: String, vararg ids: UUID) {
    val events = hmppsEventsQueue.receivePersonCaseNoteEventsOnQueue()
    assertThat(events).hasSize(ids.size)
    val personIdentifiers = events.map { it.personReference.findNomsNumber() }.toSet()
    assertThat(personIdentifiers).hasSize(1)
    assertThat(personIdentifiers.first()).isEqualTo(newNoms)
    assertThat(events.map { it.additionalInformation.previousNomsNumber to it.additionalInformation.id })
      .containsExactlyInAnyOrderElementsOf(ids.map { oldNoms to it })
  }

  companion object {
    private const val BASE_URL = "/move/case-notes"
  }
}
