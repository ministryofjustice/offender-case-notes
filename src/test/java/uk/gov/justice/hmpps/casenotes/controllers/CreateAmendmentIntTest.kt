package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi
import uk.gov.justice.hmpps.casenotes.notes.AmendCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.newId
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.prisonNumber
import java.util.UUID

class CreateAmendmentIntTest : ResourceTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.put().uri(urlToTest(), prisonNumber(), UUID.randomUUID().toString())
      .exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    amendCaseNote(
      prisonNumber(),
      UUID.randomUUID().toString(),
      amendCaseNoteRequest(),
      roles = listOf(SecurityUserContext.ROLE_CASE_NOTES_READ),
    ).expectStatus().isForbidden
  }

  @Test
  fun `cannot create case note without user details`() {
    val request = amendCaseNoteRequest()
    val response =
      amendCaseNote(prisonNumber(), UUID.randomUUID().toString(), request, tokenUsername = "NoneExistentUser")
        .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Invalid username provided in token")
    }
  }

  @Test
  fun `cannot amend a sync to nomis case note with non nomis user`() {
    val username = "DeliusUser"
    oAuthApi.subGetUserDetails(username, nomisUser = false)
    val type = getAllTypes().first { it.syncToNomis }
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber(), type))
    val response =
      amendCaseNote(
        caseNote.personIdentifier,
        caseNote.id.toString(),
        amendCaseNoteRequest(),
        mapOf(),
        tokenUsername = username,
      )
        .errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(developerMessage).isEqualTo("Unable to author 'sync to nomis' type without a nomis user")
    }
  }

  @Test
  fun `can amend a case note with write role`() {
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber()))
    val request = amendCaseNoteRequest()
    val response = amendCaseNote(caseNote.personIdentifier, caseNote.id.toString(), request).success<CaseNote>()

    assertThat(response.amendments.size).isEqualTo(1)
    assertThat(response.amendments.first().additionalNoteText).isEqualTo(request.text)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(caseNote.id, response.personIdentifier))
    with(saved.amendments().first()) {
      assertThat(text).isEqualTo(request.text)
      assertThat(authorUsername).isEqualTo(USERNAME)
    }
  }

  @Test
  fun `can amend a case note with write role using legacyId`() {
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber(), legacyId = newId()))
    val request = amendCaseNoteRequest()
    val response = amendCaseNote(caseNote.personIdentifier, caseNote.legacyId.toString(), request).success<CaseNote>()

    assertThat(response.amendments.size).isEqualTo(1)
    assertThat(response.amendments.first().additionalNoteText).isEqualTo(request.text)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(caseNote.id, response.personIdentifier))
    with(saved.amendments().first()) {
      assertThat(text).isEqualTo(request.text)
      assertThat(authorUsername).isEqualTo(USERNAME)
    }
  }

  @Test
  fun `can amend a case note without caseload id when not sync to nomis`() {
    val type = getAllTypes().first { !it.syncToNomis }
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber(), type = type))
    val request = amendCaseNoteRequest()
    val response = amendCaseNote(caseNote.personIdentifier, caseNote.id.toString(), request, caseloadId = null)
      .success<CaseNote>()

    assertThat(response.amendments.size).isEqualTo(1)
    assertThat(response.amendments.first().additionalNoteText).isEqualTo(request.text)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(caseNote.id, response.personIdentifier))
    with(saved.amendments().first()) {
      assertThat(text).isEqualTo(request.text)
      assertThat(authorUsername).isEqualTo(USERNAME)
    }
  }

  private fun amendCaseNoteRequest(
    text: String = "Some amended text about a case note",
  ) = AmendCaseNoteRequest(text)

  private fun amendCaseNote(
    prisonNumber: String,
    caseNoteId: String,
    request: AmendCaseNoteRequest,
    params: Map<String, String> = mapOf("useRestrictedType" to "true"),
    roles: List<String> = listOf(SecurityUserContext.ROLE_CASE_NOTES_WRITE),
    tokenUsername: String = USERNAME,
    caseloadId: String? = ACTIVE_PRISON,
  ) = webTestClient.put().uri { ub ->
    ub.path(urlToTest())
    params.forEach {
      ub.queryParam(it.key, it.value)
    }
    ub.build(prisonNumber, caseNoteId)
  }.headers(addBearerAuthorisation(tokenUsername, roles))
    .addHeader(CASELOAD_ID, caseloadId)
    .bodyValue(request)
    .exchange()

  private fun urlToTest() = "/case-notes/{offenderIdentifier}/{caseNoteId}"

  companion object {
    @JvmStatic
    @BeforeAll
    fun setup() {
      oAuthApi.subGetUserDetails(USERNAME)
    }
  }
}
