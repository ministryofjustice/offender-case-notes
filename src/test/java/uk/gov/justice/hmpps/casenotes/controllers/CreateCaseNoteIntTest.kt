package uk.gov.justice.hmpps.casenotes.controllers

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.LocalDateTime
import java.util.UUID.fromString

class CreateCaseNoteIntTest : ResourceTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(personIdentifier())).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    createCaseNote(
      personIdentifier(),
      createCaseNoteRequest(),
      roles = listOf(ROLE_CASE_NOTES_READ),
    ).expectStatus().isForbidden
  }

  @Test
  fun `cannot create case note without user details`() {
    val request = createCaseNoteRequest()
    val response = createCaseNote(personIdentifier(), request, tokenUsername = "NoneExistentUser")
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Invalid username provided in token")
    }
  }

  @Test
  fun `cannot create a case note with an inactive type`() {
    val type = givenRandomType(active = false, restricted = false)
    val request = createCaseNoteRequest(type = type.type.code, subType = type.code)
    val response = createCaseNote(personIdentifier(), request, params = mapOf()).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(developerMessage).isEqualTo("Case note type not active")
    }
  }

  @Test
  fun `cannot create a sync to nomis case note with non nomis user`() {
    val username = "DeliusUser"
    oAuthApi.subGetUserDetails(username, nomisUser = false)
    val type = getAllTypes().first { it.syncToNomis }
    val request = createCaseNoteRequest(type = type.type.code, subType = type.code)
    val response = createCaseNote(personIdentifier(), request, params = mapOf(), tokenUsername = username)
      .errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(developerMessage).isEqualTo("Unable to author 'sync to nomis' type without a nomis user")
    }
  }

  @Test
  fun `can create a case note with write role using jwt subject`() {
    val request = createCaseNoteRequest()
    val response = createCaseNote(personIdentifier(), request).success<CaseNote>(HttpStatus.CREATED)

    val saved = requireNotNull(
      noteRepository.findByIdAndPersonIdentifier(fromString(response.id), response.personIdentifier),
    )
    saved.verifyAgainst(request)
    assertThat(saved.authorUsername).isEqualTo(USERNAME)
    response.verifyAgainst(saved)

    hmppsEventsQueue.receiveDomainEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, saved)
  }

  @Test
  fun `400 bad request - field validation failures`() {
    val request = createCaseNoteRequest(
      type = "n".repeat(13),
      subType = "n".repeat(13),
      locationId = "n".repeat(13),
      text = "",
    )
    val response = createCaseNote(personIdentifier(), request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo(
        """
        |400 BAD_REQUEST Validation failures: 
        |location must be no more than 12 characters
        |sub type must be no more than 12 characters
        |text cannot be blank
        |type must be no more than 12 characters
        |
        """.trimMargin(),
      )
    }
  }

  @Test
  fun `can handle explicitly null value for occurrence date time`() {
    val request = """
      {
        "locationId": "MDI",
        "type": "OMIC",
        "subType": "GEN",
        "occurrenceDateTime": null,
        "text": "This should not really be happening",
        "systemGenerated": null
      }
    """.trimMargin()
    val response = createCaseNoteWithStringRequestBody(personIdentifier(), request).success<CaseNote>(HttpStatus.CREATED)

    val saved = requireNotNull(
      noteRepository.findByIdAndPersonIdentifier(fromString(response.id), response.personIdentifier),
    )
    saved.verifyAgainst(objectMapper.readValue<CreateCaseNoteRequest>(request))
    assertThat(saved.authorUsername).isEqualTo(USERNAME)
    response.verifyAgainst(saved)

    hmppsEventsQueue.receiveDomainEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, saved)
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
    tokenUsername: String = USERNAME,
  ) = webTestClient.post().uri { ub ->
    ub.path(urlToTest(prisonNumber))
    params.forEach {
      ub.queryParam(it.key, it.value)
    }
    ub.build()
  }.headers(addBearerAuthorisation(tokenUsername, roles))
    .header(CASELOAD_ID, ACTIVE_PRISON)
    .bodyValue(request)
    .exchange()

  private fun createCaseNoteWithStringRequestBody(
    prisonNumber: String,
    request: String,
    params: Map<String, String> = mapOf("useRestrictedType" to "true"),
    roles: List<String> = listOf(ROLE_CASE_NOTES_WRITE),
    tokenUsername: String = USERNAME,
  ) = webTestClient.post().uri { ub ->
    ub.path(urlToTest(prisonNumber))
    params.forEach {
      ub.queryParam(it.key, it.value)
    }
    ub.build()
  }.headers(addBearerAuthorisation(tokenUsername, roles))
    .header(CASELOAD_ID, ACTIVE_PRISON)
    .bodyValue(request)
    .exchange()

  private fun urlToTest(prisonNumber: String) = "/case-notes/$prisonNumber"

  private fun Note.verifyAgainst(request: CreateCaseNoteRequest) {
    assertThat(subType.type.code).isEqualTo(request.type)
    assertThat(subType.code).isEqualTo(request.subType)
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
