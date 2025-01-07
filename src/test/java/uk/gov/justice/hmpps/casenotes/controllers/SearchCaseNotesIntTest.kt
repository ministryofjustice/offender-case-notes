package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.of
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.legacy.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.notes.SearchNotesRequest
import uk.gov.justice.hmpps.casenotes.notes.SearchNotesResponse
import uk.gov.justice.hmpps.casenotes.notes.TypeSubTypeRequest
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.LocalDateTime

class SearchCaseNotesIntTest : IntegrationTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(personIdentifier())).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    findCaseNotesSpec(personIdentifier(), roles = listOf("ANY_OTHER_ROLE")).expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  fun `400 bad request - invalid request fields`(request: SearchNotesRequest, expected: ErrorResponse) {
    val prisonNumber = personIdentifier()

    val res = findCaseNotesSpec(prisonNumber, request).expectStatus().isBadRequest.errorResponse(HttpStatus.BAD_REQUEST)
    with(res) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo(expected.developerMessage)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_CASE_NOTES_READ, ROLE_CASE_NOTES_WRITE])
  fun `can find a case note by person identifier with appropriate role`(role: String) {
    val caseNote = givenCaseNote(generateCaseNote().withAmendment())
    val response = findCaseNotes(caseNote.personIdentifier, roles = listOf(role))

    assertThat(response.metadata.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
    first.amendments.first().verifyAgainst(caseNote.amendments().first())
  }

  @Test
  fun `can find sensitive and non-sensitive case notes`() {
    val prisonNumber = personIdentifier()
    val sensitiveType = givenRandomType(sensitive = true)
    val nonSensitiveType = givenRandomType(sensitive = false)
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber, nonSensitiveType))
    givenCaseNote(generateCaseNote(prisonNumber, sensitiveType))

    assertThat(findCaseNotes(prisonNumber, searchRequest(includeSensitive = true)).metadata.totalElements).isEqualTo(2)

    val response = findCaseNotes(prisonNumber, searchRequest(includeSensitive = false))
    assertThat(response.metadata.totalElements).isEqualTo(1)
    assertThat(response.hasCaseNotes).isTrue()
    val first = response.content.first()
    first.verifyAgainst(caseNote)
  }

  @Test
  fun `can find by occurred at`() {
    val prisonNumber = personIdentifier()
    givenCaseNote(generateCaseNote(prisonNumber, occurredAt = LocalDateTime.now().minusDays(7)))
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber, occurredAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(prisonNumber, occurredAt = LocalDateTime.now().minusDays(3)))
    assertThat(findCaseNotes(prisonNumber).metadata.totalElements).isEqualTo(3)

    val response = findCaseNotes(
      prisonNumber,
      searchRequest(occurredFrom = LocalDateTime.now().minusDays(6), occurredTo = LocalDateTime.now().minusDays(4)),
    )
    assertThat(response.metadata.totalElements).isEqualTo(1)
    assertThat(response.hasCaseNotes).isTrue()
    val first = response.content.first()
    first.verifyAgainst(caseNote)
  }

  @Test
  fun `can sort by occurred at`() {
    val prisonNumber = personIdentifier()
    givenCaseNote(generateCaseNote(prisonNumber, text = "SEVEN", occurredAt = LocalDateTime.now().minusDays(7)))
    givenCaseNote(generateCaseNote(prisonNumber, text = "FIVE", occurredAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(prisonNumber, text = "THREE", occurredAt = LocalDateTime.now().minusDays(3)))

    val response1 = findCaseNotes(prisonNumber, searchRequest(sort = "occurredAt,asc"))
    assertThat(response1.metadata.totalElements).isEqualTo(3)
    assertThat(response1.hasCaseNotes).isTrue()
    assertThat(response1.content.map { it.text }).containsExactly("SEVEN", "FIVE", "THREE")

    val response2 = findCaseNotes(prisonNumber, searchRequest(sort = "occurredAt,desc"))
    assertThat(response2.metadata.totalElements).isEqualTo(3)
    assertThat(response2.hasCaseNotes).isTrue()
    assertThat(response2.content.map { it.text }).containsExactly("THREE", "FIVE", "SEVEN")
  }

  @Test
  fun `can sort by created at`() {
    val prisonNumber = personIdentifier()
    givenCaseNote(generateCaseNote(prisonNumber, text = "SEVEN", createdAt = LocalDateTime.now().minusDays(7)))
    givenCaseNote(generateCaseNote(prisonNumber, text = "FIVE", createdAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(prisonNumber, text = "THREE", createdAt = LocalDateTime.now().minusDays(3)))

    val response1 = findCaseNotes(prisonNumber, searchRequest(sort = "createdAt,asc"))
    assertThat(response1.metadata.totalElements).isEqualTo(3)
    assertThat(response1.content.map { it.text }).containsExactly("SEVEN", "FIVE", "THREE")

    val response2 = findCaseNotes(prisonNumber, searchRequest(sort = "createdAt,desc"))
    assertThat(response2.metadata.totalElements).isEqualTo(3)
    assertThat(response2.content.map { it.text }).containsExactly("THREE", "FIVE", "SEVEN")
  }

  @Test
  fun `can find by parent type`() {
    val prisonNumber = personIdentifier()
    val types = getAllTypes().asSequence()
      .filter { !it.sensitive }
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .shuffled().toList()
    val caseNotes = types.map { givenCaseNote(generateCaseNote(prisonNumber, it)) }

    val parent = types.random().type
    val toFind = caseNotes.filter { it.subType.type.code == parent.code }
    val response = findCaseNotes(
      prisonNumber,
      searchRequest(typeSubTypes = setOf(TypeSubTypeRequest(parent.code, emptySet()))),
    )

    assertThat(response.metadata.totalElements).isEqualTo(toFind.size)
    assertThat(response.hasCaseNotes).isTrue()
    assertThat(response.content.map { it.type to it.subType })
      .containsExactlyInAnyOrderElementsOf(toFind.map { it.subType.type.code to it.subType.code })
  }

  @Test
  fun `can find by a single sub type`() {
    val prisonNumber = personIdentifier()
    val types = getAllTypes().asSequence()
      .filter { !it.sensitive }
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .toList()
    val caseNotes = types.map { givenCaseNote(generateCaseNote(prisonNumber, it)) }

    val toFind = caseNotes.random()

    val response = findCaseNotes(
      prisonNumber,
      searchRequest(typeSubTypes = setOf(TypeSubTypeRequest(toFind.subType.type.code, setOf(toFind.subType.code)))),
    )

    assertThat(response.metadata.totalElements).isEqualTo(1)
    assertThat(response.hasCaseNotes).isTrue()
    val found = response.content.first()
    assertThat(found.type).isEqualTo(toFind.subType.type.code)
    assertThat(found.typeDescription).isEqualTo(toFind.subType.type.description)
    assertThat(found.subType).isEqualTo(toFind.subType.code)
    assertThat(found.subTypeDescription).isEqualTo(toFind.subType.description)
  }

  @Test
  fun `can retrieve paginated case notes with amendments`() {
    val prisonNumber = personIdentifier()
    val types = getAllTypes().asSequence().take(30)
    types.forEach { givenCaseNote(generateCaseNote(prisonNumber, it)).withAmendment().withAmendment().withAmendment() }

    val response1 = findCaseNotes(prisonNumber, searchRequest())
    assertThat(response1.metadata.size).isEqualTo(25)
    assertThat(response1.metadata.page).isEqualTo(1)
    assertThat(response1.metadata.totalElements).isEqualTo(30)
    assertThat(response1.content.size).isEqualTo(25)
    assertThat(response1.hasCaseNotes).isTrue()

    val response2 = findCaseNotes(prisonNumber, searchRequest(page = 2, size = 20))
    assertThat(response2.metadata.size).isEqualTo(20)
    assertThat(response2.metadata.page).isEqualTo(2)
    assertThat(response2.metadata.totalElements).isEqualTo(30)
    assertThat(response2.content.size).isEqualTo(10)
    assertThat(response2.hasCaseNotes).isTrue()
  }

  @Test
  fun `when no case notes exist`() {
    val prisonNumber = personIdentifier()

    val response = findCaseNotes(prisonNumber, searchRequest())
    assertThat(response.metadata.totalElements).isEqualTo(0)
    assertThat(response.hasCaseNotes).isFalse()
  }

  @Test
  fun `when no case non-sensitive notes exist`() {
    val prisonNumber = personIdentifier()
    val type = givenRandomType(sensitive = true)
    givenCaseNote(generateCaseNote(prisonNumber, type))
    assertThat(findCaseNotes(prisonNumber, searchRequest(includeSensitive = true)).hasCaseNotes).isTrue()

    val response = findCaseNotes(prisonNumber, searchRequest(includeSensitive = false))
    assertThat(response.metadata.totalElements).isEqualTo(0)
    assertThat(response.hasCaseNotes).isFalse()
  }

  private fun urlToTest(prisonNumber: String) = "/search/case-notes/$prisonNumber"

  private fun findCaseNotesSpec(
    prisonNumber: String,
    request: SearchNotesRequest = searchRequest(),
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ) = webTestClient.post().uri(urlToTest(prisonNumber))
    .bodyValue(request)
    .headers(addBearerAuthorisation(username, roles))
    .exchange()

  private fun findCaseNotes(
    prisonNumber: String,
    request: SearchNotesRequest = searchRequest(),
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ): SearchNotesResponse = findCaseNotesSpec(prisonNumber, request, roles, username)
    .expectStatus().isOk
    .expectBody(SearchNotesResponse::class.java).returnResult().responseBody!!

  companion object {
    @JvmStatic
    fun invalidRequests() = listOf(
      of(
        searchRequest(page = 0),
        ErrorResponse(400, developerMessage = "400 BAD_REQUEST Validation failure: Page number must be at least 1"),
      ),
      of(
        searchRequest(size = 0),
        ErrorResponse(400, developerMessage = "400 BAD_REQUEST Validation failure: Page size must be at least 1"),
      ),
      of(
        searchRequest(sort = Note.PERSON_IDENTIFIER),
        ErrorResponse(400, developerMessage = "400 BAD_REQUEST Validation failure: Sort field invalid, please provide one of [occurredAt, createdAt]"),
      ),
    )

    private fun searchRequest(
      includeSensitive: Boolean = true,
      typeSubTypes: Set<TypeSubTypeRequest> = emptySet(),
      occurredFrom: LocalDateTime? = null,
      occurredTo: LocalDateTime? = null,
      page: Int = 1,
      size: Int = 25,
      sort: String = "occurredAt,desc",
    ) = SearchNotesRequest(includeSensitive, typeSubTypes, occurredFrom, occurredTo, page, size, sort)
  }
}
