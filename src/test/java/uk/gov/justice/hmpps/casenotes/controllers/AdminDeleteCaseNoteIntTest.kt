package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_ADMIN
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.DeletionCause.DELETE
import uk.gov.justice.hmpps.casenotes.domain.System
import uk.gov.justice.hmpps.casenotes.domain.audit.DeletedCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent
import uk.gov.justice.hmpps.casenotes.health.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.hmpps.casenotes.notes.DeleteCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.util.UUID

class AdminDeleteCaseNoteIntTest : IntegrationTest() {

  @Autowired
  lateinit var deletedCaseNoteRepository: DeletedCaseNoteRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.method(HttpMethod.DELETE).uri(urlToTest(UUID.randomUUID()))
      .exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    deleteCaseNote(UUID.randomUUID(), "Reason", roles = listOf(ROLE_CASE_NOTES_WRITE))
      .expectStatus().isForbidden
  }

  @Test
  fun `cannot delete case note without user details`() {
    val response = deleteCaseNote(UUID.randomUUID(), "Reason", username = "NoneExistentUser")
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Invalid username provided in token")
    }
  }

  @Test
  fun `204 not content - id to delete does not exist`() {
    deleteCaseNote(UUID.randomUUID(), "Reason").expectStatus().isNoContent
  }

  @Test
  fun `can delete a case note with admin role`() {
    val caseNote = givenCaseNote(generateCaseNote(personIdentifier()).withAmendment())
    val deletionReason = "Admin decision"
    deleteCaseNote(caseNote.id, deletionReason).expectStatus().isNoContent

    val saved = noteRepository.findByIdAndPersonIdentifier(caseNote.id, caseNote.personIdentifier)
    assertThat(saved).isNull()
    val deleted = deletedCaseNoteRepository.findByCaseNoteId(caseNote.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(DELETE)
    assertThat(deleted.system).isEqualTo(System.DPS)
    assertThat(deleted.reason).isEqualTo(deletionReason)
    deleted.caseNote.verifyAgainst(caseNote)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.DELETED, Source.DPS, caseNote)
  }

  private fun deleteCaseNote(
    caseNoteId: UUID,
    reason: String,
    roles: List<String> = listOf(ROLE_CASE_NOTES_ADMIN),
    username: String = USERNAME,
  ): WebTestClient.ResponseSpec = webTestClient.method(HttpMethod.DELETE)
    .uri(urlToTest(caseNoteId))
    .bodyValue(DeleteCaseNoteRequest(reason))
    .headers(addBearerAuthorisation(username, roles))
    .exchange()

  private fun urlToTest(caseNoteId: UUID) = "/admin/case-notes/$caseNoteId"

  companion object {
    @JvmStatic
    @BeforeAll
    fun setup() {
      manageUsersApi.stubGetUserDetails(USERNAME)
    }
  }
}
