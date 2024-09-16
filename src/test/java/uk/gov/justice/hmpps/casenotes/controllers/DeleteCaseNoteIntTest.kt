package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.domain.audit.DeletedCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.prisonNumber
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.util.UUID

class DeleteCaseNoteIntTest : ResourceTest() {

  @Autowired
  lateinit var deletedCaseNoteRepository: DeletedCaseNoteRepository

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
    val deleted = deletedCaseNoteRepository.findByCaseNoteId(caseNote.id)
    assertThat(deleted!!.caseNote).isNotNull()
    deleted.caseNote.verifyAgainst(caseNote)
  }

  @Test
  fun `can delete using legacy api`() {
    val type = getAllTypes().first { !it.syncToNomis }
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber(), type).withAmendment())
    deleteCaseNote(
      caseNote.personIdentifier,
      caseNote.id.toString(),
      caseloadId = null,
      roles = listOf("ROLE_DELETE_SENSITIVE_CASE_NOTES"),
    ).expectStatus().isOk

    val saved = noteRepository.findByIdAndPersonIdentifier(caseNote.id, caseNote.personIdentifier)
    assertThat(saved).isNull()
    val deleted = deletedCaseNoteRepository.findByCaseNoteId(caseNote.id)
    assertThat(deleted!!.caseNote).isNotNull()
    deleted.caseNote.verifyAgainst(caseNote)
  }

  private fun deleteCaseNote(
    prisonNumber: String,
    caseNoteId: String,
    roles: List<String> = listOf(ROLE_CASE_NOTES_WRITE),
    username: String = USERNAME,
    caseloadId: String? = ACTIVE_PRISON,
  ) = webTestClient.delete().uri(urlToTest(prisonNumber, caseNoteId))
    .headers(addBearerAuthorisation(username, roles))
    .headers { if (caseloadId != null) it[CASELOAD_ID] = caseloadId }
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
