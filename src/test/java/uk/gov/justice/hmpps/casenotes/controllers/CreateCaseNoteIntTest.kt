package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.internal.Note
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.prisonNumber
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.LocalDateTime
import java.util.UUID.fromString

class CreateCaseNoteIntTest : ResourceTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(prisonNumber())).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    createCaseNote(
      prisonNumber(),
      createCaseNoteRequest(),
      roles = listOf(ROLE_CASE_NOTES_READ),
    ).expectStatus().isForbidden
  }

  @Test
  fun `cannot create case note without user details`() {
    val request = createCaseNoteRequest()
    val response = createCaseNote(prisonNumber(), request, username = "NoneExistentUser")
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Invalid username provided in token")
    }
  }

  @Test
  fun `can create a case note with write role`() {
    val request = createCaseNoteRequest()
    val response = createCaseNote(prisonNumber(), request).success<CaseNote>(HttpStatus.CREATED)

    val saved = requireNotNull(
      noteRepository.findByIdAndPrisonNumber(fromString(response.caseNoteId), response.offenderIdentifier),
    )
    saved.verifyAgainst(request)
    response.verifyAgainst(saved)
  }

  @Test
  fun `cannot create a restricted case note without passing useRestrictedType`() {
    val type = givenRandomType(restricted = true)
    val request = createCaseNoteRequest(type = type.parentType.code, subType = type.code)
    createCaseNote(prisonNumber(), request, params = mapOf()).errorResponse(HttpStatus.FORBIDDEN)
  }

  @Test
  fun `cannot create a case note with an inactive type`() {
    val type = givenRandomType(active = false, restricted = false)
    val request = createCaseNoteRequest(type = type.parentType.code, subType = type.code)
    val response = createCaseNote(prisonNumber(), request, params = mapOf()).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(developerMessage).isEqualTo("Case note type not active")
    }
  }

  private fun createCaseNoteRequest(
    locationId: String? = "MDI",
    type: String = "OMIC",
    subType: String = "GEN",
    occurrenceDateTime: LocalDateTime = LocalDateTime.now(),
    text: String = "Some text about the case note",
    systemGenerated: Boolean? = null,
  ) = CreateCaseNoteRequest(locationId, type, subType, occurrenceDateTime, text, systemGenerated)

  private fun createCaseNote(
    prisonNumber: String,
    request: CreateCaseNoteRequest,
    params: Map<String, String> = mapOf("useRestrictedType" to "true"),
    roles: List<String> = listOf(ROLE_CASE_NOTES_WRITE),
    username: String = USERNAME,
  ) = webTestClient.post().uri { ub ->
    ub.path(urlToTest(prisonNumber))
    params.forEach {
      ub.queryParam(it.key, it.value)
    }
    ub.build()
  }.headers(addBearerAuthorisation(username, roles))
    .header(CASELOAD_ID, ACTIVE_PRISON)
    .bodyValue(request)
    .exchange()

  private fun urlToTest(prisonNumber: String) = "/case-notes/$prisonNumber"

  private fun Note.verifyAgainst(request: CreateCaseNoteRequest) {
    assertThat(type.category.code).isEqualTo(request.type)
    assertThat(type.code).isEqualTo(request.subType)
    assertThat(text).isEqualTo(request.text)
  }

  companion object {
    @JvmStatic
    @BeforeAll
    fun setup() {
      oAuthApi.subGetUserDetails(USERNAME)
    }
  }
}
