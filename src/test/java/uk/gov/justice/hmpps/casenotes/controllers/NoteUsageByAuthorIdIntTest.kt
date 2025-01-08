package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.of
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.legacy.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.notes.LatestNote
import uk.gov.justice.hmpps.casenotes.notes.NoteUsageResponse
import uk.gov.justice.hmpps.casenotes.notes.TypeSubTypeRequest
import uk.gov.justice.hmpps.casenotes.notes.UsageByAuthorIdRequest
import uk.gov.justice.hmpps.casenotes.notes.UsageByAuthorIdResponse
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.newId
import java.time.LocalDateTime

class NoteUsageByAuthorIdIntTest : IntegrationTest() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(USAGE_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    getUsageByAuthorIdSpec(
      request = UsageByAuthorIdRequest(
        typeSubTypes = setOf(TypeSubTypeRequest("T1")),
        authorIds = setOf("12345"),
      ),
      roles = listOf("ANY_OTHER_ROLE"),
    ).expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("invalidPiUsageRequest")
  fun `400 bad request - invalid request for usage request by author id`(
    request: UsageByAuthorIdRequest,
    error: ErrorResponse,
  ) {
    val res = getUsageByAuthorIdSpec(request).expectStatus().isBadRequest.errorResponse(HttpStatus.BAD_REQUEST)
    with(res) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo(error.developerMessage)
    }
  }

  @Test
  fun `200 ok - can find counts of type and subtype for multiple author ids`() {
    val author1 = newId().toString()
    val author2 = newId().toString()
    val types = getAllTypes()
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .toList()
    val caseNotes = (0..40).flatMap {
      val type = types.random()
      listOf(
        givenCaseNote(generateCaseNote(type = type, authorUserId = author1)),
        givenCaseNote(generateCaseNote(type = type, authorUserId = author2)),
      )
    }

    val toFind = caseNotes.random().let { Pair(it.subType.typeCode, it.subType.code) }

    val response = getUsageByAuthorId(
      UsageByAuthorIdRequest(
        authorIds = setOf(author1, author2),
        typeSubTypes = setOf(TypeSubTypeRequest(toFind.first, setOf(toFind.second))),
      ),
    )

    assertThat(response.content).hasSize(2)
    assertThat(response.content[author1]!!.first().count).isEqualTo(
      caseNotes.count { it.authorUserId == author1 && it.subType.typeCode == toFind.first && it.subType.code == toFind.second },
    )
    assertThat(response.content[author2]!!.first().count).isEqualTo(
      caseNotes.count { it.authorUserId == author2 && it.subType.typeCode == toFind.first && it.subType.code == toFind.second },
    )
  }

  @Test
  fun `200 ok - can find counts of multiple types and subtypes`() {
    val authorId = newId().toString()
    val types = getAllTypes()
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .toList()
    val caseNotes = (0..10).map { givenCaseNote(generateCaseNote(type = types.random(), authorUserId = authorId)) }

    val toFind = caseNotes.map { Pair(it.subType.typeCode, it.subType.code) }.toSet().take(2)

    val response = getUsageByAuthorId(
      UsageByAuthorIdRequest(
        authorIds = setOf(authorId),
        typeSubTypes = toFind.map { TypeSubTypeRequest(it.first, setOf(it.second)) }.toSet(),
      ),
    )

    assertThat(response.content).hasSize(1)
    with(response.content[authorId]!!) {
      assertThat(size).isEqualTo(2)
      forEach { usage ->
        val matching = caseNotes.filter { it.subType.typeCode == usage.type && it.subType.code == usage.subType }
        assertThat(usage.count).isEqualTo(matching.size)
        assertThat(usage.latestNote).isEqualTo(
          matching.maxBy { it.occurredAt }.let { LatestNote(it.id, it.occurredAt) },
        )
      }
    }
  }

  @Test
  fun `can find by occurred at`() {
    val authorId = newId().toString()
    val subType = givenRandomType()
    givenCaseNote(
        generateCaseNote(
            type = subType,
            occurredAt = LocalDateTime.now().minusDays(7),
            authorUserId = authorId,
        ),
    )
    val caseNote =
      givenCaseNote(
          generateCaseNote(
              type = subType,
              occurredAt = LocalDateTime.now().minusDays(5),
              authorUserId = authorId,
          ),
      )
    givenCaseNote(
        generateCaseNote(
            type = subType,
            occurredAt = LocalDateTime.now().minusDays(3),
            authorUserId = authorId,
        ),
    )

    val all = getUsageByAuthorId(
      UsageByAuthorIdRequest(
        authorIds = setOf(authorId),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
      ),
    )
    assertThat(all.content[authorId]!!.first().count).isEqualTo(3)

    val response = getUsageByAuthorId(
      UsageByAuthorIdRequest(
        authorIds = setOf(authorId),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
        occurredFrom = LocalDateTime.now().minusDays(6),
        occurredTo = LocalDateTime.now().minusDays(4),
      ),
    )

    assertThat(response.content[authorId]!!.first().count).isEqualTo(1)
    assertThat(response.content[authorId]!!.first().latestNote).isEqualTo(
      LatestNote(
        caseNote.id,
        caseNote.occurredAt,
      ),
    )
  }

  private fun getUsageByAuthorIdSpec(
    request: UsageByAuthorIdRequest = UsageByAuthorIdRequest(),
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ) = webTestClient.post().uri(USAGE_URL)
    .bodyValue(request)
    .headers(addBearerAuthorisation(username, roles))
    .exchange()

  private fun getUsageByAuthorId(
    request: UsageByAuthorIdRequest = UsageByAuthorIdRequest(),
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ): NoteUsageResponse<UsageByAuthorIdResponse> = getUsageByAuthorIdSpec(request, roles, username)
    .expectStatus().isOk
    .expectBody(object : ParameterizedTypeReference<NoteUsageResponse<UsageByAuthorIdResponse>>() {})
    .returnResult().responseBody!!

  companion object {
    private const val USAGE_URL = "/case-notes/staff-usage"

    @JvmStatic
    fun invalidPiUsageRequest() = listOf(
      of(
        UsageByAuthorIdRequest(authorIds = setOf("12345")),
        ErrorResponse(400, developerMessage = "400 BAD_REQUEST Validation failure: At least one type is required"),
      ),
      of(
        UsageByAuthorIdRequest(typeSubTypes = setOf(TypeSubTypeRequest("T1", setOf()))),
        ErrorResponse(
          400,
          developerMessage = "400 BAD_REQUEST Validation failure: At least one author id is required",
        ),
      ),
    )
  }
}
