package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.SearchNotesByIdsRequest
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.util.UUID

class SearchCaseNotesByIdIntTest : IntegrationTest() {
  @Test
  fun `401 unauthorised`() {
    findCaseNotesByIds(listOf(UUID.randomUUID()), role = null)
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    findCaseNotesByIds(listOf(UUID.randomUUID()), role = "ANY_OTHER_ROLE")
      .expectStatus().isForbidden
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_CASE_NOTES_READ, ROLE_CASE_NOTES_WRITE])
  fun `can find case notes with appropriate role`(role: String) {
    val caseNotes = noteRepository.saveAll(
      listOf(
        generateCaseNote(locationId = "LEI").withAmendment(),
        generateCaseNote(locationId = "LEI"),
        generateCaseNote(locationId = "LEI"),
        generateCaseNote(locationId = "MDI"), // won’t be requested
      ),
    )

    val ids = caseNotes.map { it.id }
      .take(3)
      .shuffled()
      .toMutableList()
    ids.add(UUID.randomUUID())
    val response = findCaseNotesByIds(ids, role = role)
      .expectBody<List<CaseNote>>()
      .returnResult().responseBody

    assertNotNull(response)
    assertThat(response).hasSize(3)
    assertThat(response).allMatch { it.locationId == "LEI" }

    val responseWithAmendment = response.find { it.amendments.isNotEmpty() }!!
    val caseNoteWithAmendment = caseNotes.find { it.amendments().isNotEmpty() }!!
    responseWithAmendment.verifyAgainst(caseNoteWithAmendment)
    responseWithAmendment.amendments[0].verifyAgainst(caseNoteWithAmendment.amendments().first())
  }

  @Test
  fun `when no case notes exist`() {
    val response = findCaseNotesByIds(listOf(UUID.randomUUID()))
      .expectBody<List<CaseNote>>()
      .returnResult().responseBody
    assertThat(response).isEmpty()
  }

  @Test
  fun `400 bad request when no ids provided`() {
    val response = findCaseNotesByIds(emptyList())
      .expectStatus().isBadRequest
      .errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(response.developerMessage).isEqualTo("400 BAD_REQUEST Validation failure: must not be empty")
  }

  private fun findCaseNotesByIds(
    ids: List<UUID>,
    role: String? = ROLE_CASE_NOTES_READ,
    username: String = USERNAME,
  ) = webTestClient.post().uri("/search/case-notes/by-ids")
    .apply {
      if (role != null) {
        headers(addBearerAuthorisation(username, listOf(role)))
      }
    }
    .bodyValue(SearchNotesByIdsRequest(ids))
    .exchange()
}
