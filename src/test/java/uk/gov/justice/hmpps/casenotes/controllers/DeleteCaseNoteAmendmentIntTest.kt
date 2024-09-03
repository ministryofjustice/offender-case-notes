package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi
import uk.gov.justice.hmpps.casenotes.notes.internal.AmendmentRepository
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.prisonNumber

class DeleteCaseNoteAmendmentIntTest : ResourceTest() {

  @Autowired
  lateinit var amendmentRepository: AmendmentRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(prisonNumber(), NomisIdGenerator.newId()))
      .exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    deleteAmendment(
      prisonNumber(),
      NomisIdGenerator.newId(),
      roles = listOf(ROLE_CASE_NOTES_READ),
    ).expectStatus().isForbidden
  }

  @Test
  fun `cannot delete case note without user details`() {
    val response = deleteAmendment(prisonNumber(), NomisIdGenerator.newId(), username = "NoneExistentUser")
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Invalid username provided in token")
    }
  }

  @Test
  fun `can delete an amendment with write role`() {
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber()).withAmendment())
    val amendment = caseNote.amendments().first()
    deleteAmendment(caseNote.prisonNumber, amendment.id!!).expectStatus().isOk

    val saved = amendmentRepository.findByIdOrNull(amendment.id)
    assertThat(saved).isNull()
  }

  private fun deleteAmendment(
    prisonNumber: String,
    amendmentId: Long,
    roles: List<String> = listOf(ROLE_CASE_NOTES_WRITE),
    username: String = USERNAME,
  ) = webTestClient.delete().uri(urlToTest(prisonNumber, amendmentId))
    .headers(addBearerAuthorisation(username, roles))
    .header(CASELOAD_ID, ACTIVE_PRISON)
    .exchange()

  private fun urlToTest(prisonNumber: String, amendmentId: Long) = "/case-notes/amendment/$prisonNumber/$amendmentId"

  companion object {
    @JvmStatic
    @BeforeAll
    fun setup() {
      oAuthApi.subGetUserDetails(USERNAME)
    }
  }
}
