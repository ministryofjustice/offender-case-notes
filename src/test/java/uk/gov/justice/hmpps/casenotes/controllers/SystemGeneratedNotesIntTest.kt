package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_SYSTEM_GENERATED_RW
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.systemgenerated.SystemGeneratedRequest
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.prisonNumber
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class SystemGeneratedNotesIntTest : ResourceTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(prisonNumber())).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    sysGenNote(prisonNumber(), sysGenRequest(), roles = listOf(ROLE_CASE_NOTES_WRITE)).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - case note type must exist`() {
    val request = sysGenRequest(type = "NON_EXISTENT", subType = "NOT_THERE")
    val response = sysGenNote(prisonNumber(), request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Unknown case note type NON_EXISTENT:NOT_THERE")
    }
  }

  @Test
  fun `400 bad request - case note types cannot be sync to nomis type`() {
    val type = getAllTypes().filter { it.syncToNomis }.random()
    val request = sysGenRequest(type = type.parent.code, subType = type.code)
    val response = sysGenNote(prisonNumber(), request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("System generated case notes cannot use a sync to nomis type")
    }
  }

  @Test
  fun `201 ok - new case note is correctly stored`() {
    val type = getAllTypes().filter { it.parentCode == "ACP" && !it.syncToNomis }.random()
    val personIdentifier = prisonNumber()

    val request = sysGenRequest(
      type = type.parent.code,
      subType = type.code,
      authorName = "A. P. User",
      authorUsername = "SystemUser",
      occurrenceDateTime = LocalDateTime.now().minusDays(10),
    )

    val apiClientId = "ApiClientId"
    val response = sysGenNote(personIdentifier, request, username = apiClientId).success<CaseNote>(HttpStatus.CREATED)
    val saved = noteRepository.findByIdAndPrisonNumber(UUID.fromString(response.caseNoteId), personIdentifier)
    requireNotNull(saved).verifyAgainst(request, apiClientId)
  }

  @Test
  fun `201 ok - new case note stored when optional fields are null`() {
    val type = getAllTypes().filter { it.parentCode == "ACP" && !it.syncToNomis }.random()
    val personIdentifier = prisonNumber()

    val request = sysGenRequest(
      type = type.parent.code,
      subType = type.code,
      authorName = "A. P. User",
      authorUsername = null,
      occurrenceDateTime = null,
    )

    val apiClientId = "ApiClientId"
    val response = sysGenNote(personIdentifier, request, username = apiClientId).success<CaseNote>(HttpStatus.CREATED)
    val saved = noteRepository.findByIdAndPrisonNumber(UUID.fromString(response.caseNoteId), personIdentifier)
    requireNotNull(saved).verifyAgainst(request, apiClientId)
  }

  private fun Note.verifyAgainst(request: SystemGeneratedRequest, userId: String) {
    assertThat(type.parent.code).isEqualTo(request.type)
    assertThat(type.code).isEqualTo(request.subType)
    assertThat(text).isEqualTo(request.text)
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
  locationId: String = "MDI",
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