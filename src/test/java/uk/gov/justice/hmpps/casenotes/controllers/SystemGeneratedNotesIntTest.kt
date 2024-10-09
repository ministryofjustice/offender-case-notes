package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_SYSTEM_GENERATED_RW
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi
import uk.gov.justice.hmpps.casenotes.health.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.systemgenerated.SystemGeneratedRequest
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class SystemGeneratedNotesIntTest : IntegrationTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(personIdentifier())).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    sysGenNote(personIdentifier(), sysGenRequest(), roles = listOf(ROLE_CASE_NOTES_WRITE)).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - case note type must exist`() {
    val request = sysGenRequest(type = "NON_EXISTENT", subType = "NOT_THERE")
    val response = sysGenNote(personIdentifier(), request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Unknown case note type NON_EXISTENT:NOT_THERE")
    }
  }

  @Test
  fun `400 bad request - case note types cannot be sync to nomis type`() {
    val type = getAllTypes().filter { it.syncToNomis }.random()
    val request = sysGenRequest(type = type.type.code, subType = type.code)
    val response = sysGenNote(personIdentifier(), request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("System generated case notes cannot use a sync to nomis type")
    }
  }

  @Test
  fun `400 bad request - field validation failures`() {
    val request = sysGenRequest(
      type = "n".repeat(13),
      subType = "n".repeat(13),
      locationId = "n".repeat(13),
      authorUsername = "n".repeat(65),
      authorName = "n".repeat(81),
      text = "",
    )
    val response = sysGenNote(personIdentifier(), request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo(
        """
        |400 BAD_REQUEST Validation failures: 
        |author name cannot be more than 80 characters
        |author username cannot be more than 64 characters
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
  fun `201 ok - new case note is correctly stored`() {
    val type = getAllTypes().filter { it.typeCode == "ACP" && !it.syncToNomis }.random()
    val personIdentifier = personIdentifier()

    val request = sysGenRequest(
      type = type.type.code,
      subType = type.code,
      authorName = "A. P. User",
      authorUsername = "SystemUser",
      occurrenceDateTime = LocalDateTime.now().minusDays(10),
    )

    val apiClientId = "ApiClientId"
    val response = sysGenNote(personIdentifier, request, username = apiClientId).success<CaseNote>(HttpStatus.CREATED)
    val saved = noteRepository.findByIdAndPersonIdentifier(UUID.fromString(response.id), personIdentifier)
    requireNotNull(saved).verifyAgainst(request, apiClientId)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, saved)
  }

  @Test
  fun `201 ok - new case note stored when optional fields are null`() {
    val type = getAllTypes().filter { it.typeCode == "ACP" && !it.syncToNomis }.random()
    val personIdentifier = personIdentifier()

    val request = sysGenRequest(
      type = type.type.code,
      subType = type.code,
      authorName = "A. P. User",
      authorUsername = null,
      occurrenceDateTime = null,
    )

    val apiClientId = "ApiClientId"
    val response = sysGenNote(personIdentifier, request, username = apiClientId).success<CaseNote>(HttpStatus.CREATED)
    val saved = noteRepository.findByIdAndPersonIdentifier(UUID.fromString(response.id), personIdentifier)
    requireNotNull(saved).verifyAgainst(request, apiClientId)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, saved)
  }

  @Test
  fun `201 ok - use prison id from prisoner search when location id not provided`() {
    val personIdentifier = personIdentifier()
    val prisonId = "LEI"
    oAuthApi.stubGrantToken()
    prisonerSearchApi.stubPrisonerDetails(personIdentifier, prisonId)
    val type = getAllTypes().filter { it.typeCode == "ACP" && !it.syncToNomis }.random()

    val request = sysGenRequest(
      type = type.type.code,
      subType = type.code,
      occurrenceDateTime = null,
      locationId = null,
    )

    val apiClientId = "API_CLIENT_ID"
    val response = sysGenNote(personIdentifier, request).success<CaseNote>(HttpStatus.CREATED)
    assertThat(response.locationId).isEqualTo(prisonId)
    val saved = noteRepository.findByIdAndPersonIdentifier(UUID.fromString(response.id), personIdentifier)
    requireNotNull(saved).verifyAgainst(request, apiClientId)
    assertThat(saved.locationId).isEqualTo(prisonId)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, saved)
  }

  private fun Note.verifyAgainst(request: SystemGeneratedRequest, userId: String) {
    assertThat(subType.type.code).isEqualTo(request.type)
    assertThat(subType.code).isEqualTo(request.subType)
    assertThat(text).isEqualTo(request.text)
    request.locationId?.also { assertThat(locationId).isEqualTo(it) }
    request.occurrenceDateTime?.also {
      assertThat(occurredAt.truncatedTo(SECONDS)).isEqualTo(it.truncatedTo(SECONDS))
    }
    assertThat(authorName).isEqualTo(request.authorName)
    (request.authorUsername ?: userId).also { assertThat(authorUsername).isEqualTo(it) }
    assertThat(authorUserId).isEqualTo(userId)
  }

  private fun sysGenNote(
    personIdentifier: String,
    request: SystemGeneratedRequest,
    roles: List<String> = listOf(ROLE_SYSTEM_GENERATED_RW),
    username: String = "API_CLIENT_ID",
  ) = webTestClient.post().uri(urlToTest(personIdentifier))
    .headers(addBearerAuthorisation(username, roles))
    .bodyValue(request)
    .exchange()

  private fun urlToTest(personIdentifier: String) = "/system-generated/case-notes/$personIdentifier"
}

private fun sysGenRequest(
  locationId: String? = "MDI",
  type: String = "ACP",
  subType: String = "REFD",
  occurrenceDateTime: LocalDateTime? = LocalDateTime.now(),
  text: String = "This is a system generated (auto-generated) case note",
  authorUsername: String? = null,
  authorName: String = "A display name for the client",
) = SystemGeneratedRequest(
  locationId,
  type,
  subType,
  occurrenceDateTime,
  authorUsername,
  authorName,
  text,
)
