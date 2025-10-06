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
import uk.gov.justice.hmpps.casenotes.notes.AuthorIdentifierType
import uk.gov.justice.hmpps.casenotes.notes.AuthorNotesResponse
import uk.gov.justice.hmpps.casenotes.notes.SearchNotesRequest
import uk.gov.justice.hmpps.casenotes.notes.TypeSubTypeRequest
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.newId
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.LocalDateTime

class SearchAuthorNotesIntTest : IntegrationTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(AUTHOR_SEARCH_URL, "NE1", "123456").exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    findAuthorNotesSpec("NE1", "123456", roles = listOf("ANY_OTHER_ROLE")).expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  fun `400 bad request - invalid request fields`(request: SearchNotesRequest, expected: ErrorResponse) {
    val prisonCode = "VAL"
    val authorUsername = "US37R"
    val res = findAuthorNotesSpec(prisonCode, authorUsername, request = request)
      .expectStatus().isBadRequest
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(res) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo(expected.developerMessage)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_CASE_NOTES_READ, ROLE_CASE_NOTES_WRITE])
  fun `can find a case note by author with appropriate role`(role: String) {
    val caseNote = givenCaseNote(generateCaseNote().withAmendment())
    val response = findAuthorNotes(caseNote.locationId, caseNote.authorUserId, roles = listOf(role))

    assertThat(response.metadata.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
    first.amendments.first().verifyAgainst(caseNote.amendments().first())
  }

  @Test
  fun `can find sensitive and non-sensitive case notes`() {
    val authorId = newId().toString()
    val nonSensitiveType = givenRandomType(sensitive = false)
    val caseNote = givenCaseNote(generateCaseNote(type = nonSensitiveType, authorUserId = authorId))
    val sensitiveType = givenRandomType(sensitive = true)
    givenCaseNote(generateCaseNote(type = sensitiveType, authorUserId = authorId))

    assertThat(
      findAuthorNotes(
        caseNote.locationId,
        authorId,
        searchRequest(includeSensitive = true),
      ).metadata.totalElements,
    ).isEqualTo(2)

    val response = findAuthorNotes(caseNote.locationId, authorId, searchRequest(includeSensitive = false))
    assertThat(response.metadata.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
  }

  @Test
  fun `can find by occurred at`() {
    val authorUsername = "T35T"
    givenCaseNote(generateCaseNote(authorUsername = authorUsername, occurredAt = LocalDateTime.now().minusDays(7)))
    val caseNote =
      givenCaseNote(generateCaseNote(authorUsername = authorUsername, occurredAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(authorUsername = authorUsername, occurredAt = LocalDateTime.now().minusDays(3)))
    assertThat(findAuthorNotes(caseNote.locationId, authorUsername, authorIdType = AuthorIdentifierType.USERNAME).metadata.totalElements).isEqualTo(3)

    val response = findAuthorNotes(
      caseNote.locationId,
      authorUsername,
      searchRequest(occurredFrom = LocalDateTime.now().minusDays(6), occurredTo = LocalDateTime.now().minusDays(4)),
      AuthorIdentifierType.USERNAME,
    )
    assertThat(response.metadata.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
  }

  @Test
  fun `can sort by occurred at`() {
    val authorId = newId().toString()
    givenCaseNote(
      generateCaseNote(
        authorUserId = authorId,
        text = "SEVEN",
        occurredAt = LocalDateTime.now().minusDays(7),
      ),
    )
    givenCaseNote(
      generateCaseNote(
        authorUserId = authorId,
        text = "FIVE",
        occurredAt = LocalDateTime.now().minusDays(5),
      ),
    )
    givenCaseNote(
      generateCaseNote(
        authorUserId = authorId,
        text = "THREE",
        occurredAt = LocalDateTime.now().minusDays(3),
      ),
    )

    val response1 = findAuthorNotes("MDI", authorId, searchRequest(sort = "occurredAt,asc"))
    assertThat(response1.metadata.totalElements).isEqualTo(3)
    assertThat(response1.content.map { it.text }).containsExactly("SEVEN", "FIVE", "THREE")

    val response2 = findAuthorNotes("MDI", authorId, searchRequest(sort = "occurredAt,desc"))
    assertThat(response2.metadata.totalElements).isEqualTo(3)
    assertThat(response2.content.map { it.text }).containsExactly("THREE", "FIVE", "SEVEN")
  }

  @Test
  fun `can sort by created at`() {
    val authorId = newId().toString()
    val locationId = "SCA"
    givenCaseNote(
      generateCaseNote(
        locationId = locationId,
        authorUserId = authorId,
        text = "SEVEN",
        createdAt = LocalDateTime.now().minusDays(7),
      ),
    )
    givenCaseNote(
      generateCaseNote(
        locationId = locationId,
        authorUserId = authorId,
        text = "FIVE",
        createdAt = LocalDateTime.now().minusDays(5),
      ),
    )
    givenCaseNote(
      generateCaseNote(
        locationId = locationId,
        authorUserId = authorId,
        text = "THREE",
        createdAt = LocalDateTime.now().minusDays(3),
      ),
    )

    val response1 = findAuthorNotes(locationId, authorId, searchRequest(sort = "createdAt,asc"))
    assertThat(response1.metadata.totalElements).isEqualTo(3)
    assertThat(response1.content.map { it.text }).containsExactly("SEVEN", "FIVE", "THREE")

    val response2 = findAuthorNotes(locationId, authorId, searchRequest(sort = "createdAt,desc"))
    assertThat(response2.metadata.totalElements).isEqualTo(3)
    assertThat(response2.content.map { it.text }).containsExactly("THREE", "FIVE", "SEVEN")
  }

  @Test
  fun `can find by parent type`() {
    val locationId = "PTS"
    val authorId = newId().toString()
    val types = getAllTypes().asSequence()
      .filter { !it.sensitive }
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .shuffled().toList()
    val caseNotes =
      types.map { givenCaseNote(generateCaseNote(locationId = locationId, authorUserId = authorId, type = it)) }

    val parent = types.random().type
    val toFind = caseNotes.filter { it.subType.type.code == parent.code }
    val response = findAuthorNotes(
      locationId,
      authorId,
      searchRequest(typeSubTypes = setOf(TypeSubTypeRequest(parent.code, emptySet()))),
    )

    assertThat(response.metadata.totalElements).isEqualTo(toFind.size)
    assertThat(response.content.map { it.type to it.subType })
      .containsExactlyInAnyOrderElementsOf(toFind.map { it.subType.type.code to it.subType.code })
  }

  @Test
  fun `can find by a single sub type`() {
    val authorId = newId().toString()
    val locationId = newId().toString()
    val types = getAllTypes().asSequence()
      .filter { !it.sensitive }
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .toList()
    val caseNotes =
      types.map { givenCaseNote(generateCaseNote(locationId = locationId, authorUserId = authorId, type = it)) }

    val toFind = caseNotes.random()

    val response = findAuthorNotes(
      locationId,
      authorId,
      searchRequest(typeSubTypes = setOf(TypeSubTypeRequest(toFind.subType.type.code, setOf(toFind.subType.code)))),
    )

    assertThat(response.metadata.totalElements).isEqualTo(1)
    val found = response.content.first()
    assertThat(found.type).isEqualTo(toFind.subType.type.code)
    assertThat(found.typeDescription).isEqualTo(toFind.subType.type.description)
    assertThat(found.subType).isEqualTo(toFind.subType.code)
    assertThat(found.subTypeDescription).isEqualTo(toFind.subType.description)
  }

  @Test
  fun `when no case notes exist`() {
    val authorId = newId().toString()
    val locationId = newId().toString()

    val response = findAuthorNotes(locationId, authorId, searchRequest())
    assertThat(response.metadata.totalElements).isEqualTo(0)
  }

  private fun findAuthorNotesSpec(
    prisonCode: String,
    authorId: String,
    request: SearchNotesRequest = searchRequest(),
    authorIdType: AuthorIdentifierType = AuthorIdentifierType.AUTHOR_ID,
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ) = webTestClient.post().uri {
    it.path(AUTHOR_SEARCH_URL)
    it.queryParam("authorIdentifierType", authorIdType)
    it.build(prisonCode, authorId)
  }.bodyValue(request)
    .headers(addBearerAuthorisation(username, roles))
    .exchange()

  private fun findAuthorNotes(
    prisonCode: String,
    authorId: String,
    request: SearchNotesRequest = searchRequest(),
    authorIdType: AuthorIdentifierType = AuthorIdentifierType.AUTHOR_ID,
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ): AuthorNotesResponse = findAuthorNotesSpec(prisonCode, authorId, request, authorIdType, roles, username)
    .expectStatus().isOk
    .expectBody(AuthorNotesResponse::class.java).returnResult().responseBody!!

  companion object {
    const val AUTHOR_SEARCH_URL = "/search/case-notes/prisons/{prisonCode}/authors/{authorIdentifier}"

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
        ErrorResponse(
          400,
          developerMessage = "400 BAD_REQUEST Validation failure: Sort field invalid, please provide one of [occurredAt, createdAt]",
        ),
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
