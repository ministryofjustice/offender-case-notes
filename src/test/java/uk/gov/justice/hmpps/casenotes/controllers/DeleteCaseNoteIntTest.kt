package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.prisonNumber
import java.util.UUID

class DeleteCaseNoteIntTest : ResourceTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(prisonNumber(), UUID.randomUUID().toString()))
      .exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    deleteCaseNote(
      prisonNumber(),
      UUID.randomUUID().toString(),
      roles = listOf(ROLE_CASE_NOTES_READ),
    ).expectStatus().isForbidden
  }

  @Test
  fun `cannot delete case note without user details`() {
    val response = deleteCaseNote(prisonNumber(), UUID.randomUUID().toString(), username = "NoneExistentUser")
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Invalid username provided in token")
    }
  }

  @Test
  fun `can delete a case note with write role`() {
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber()).withAmendment())
    deleteCaseNote(caseNote.personIdentifier, caseNote.id.toString()).expectStatus().isOk

    val saved = noteRepository.findByIdAndPersonIdentifier(caseNote.id, caseNote.personIdentifier)
    assertThat(saved).isNull()
  }

  private fun deleteCaseNote(
    prisonNumber: String,
    caseNoteId: String,
    roles: List<String> = listOf(ROLE_CASE_NOTES_WRITE),
    username: String = USERNAME,
  ) = webTestClient.delete().uri(urlToTest(prisonNumber, caseNoteId))
    .headers(addBearerAuthorisation(username, roles))
    .header(CASELOAD_ID, ACTIVE_PRISON)
    .exchange()

  private fun urlToTest(prisonNumber: String, caseNoteId: String) = "/case-notes/$prisonNumber/$caseNoteId"

  companion object {
    @JvmStatic
    @BeforeAll
    fun setup() {
      oAuthApi.subGetUserDetails(USERNAME)
    }
  }
}
