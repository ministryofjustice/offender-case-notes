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
import uk.gov.justice.hmpps.casenotes.notes.UsageByPrisonCodeRequest
import uk.gov.justice.hmpps.casenotes.notes.UsageByPrisonCodeResponse
import java.time.LocalDateTime

class NoteUsageByPrisonCodeIntTest : IntegrationTest() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(USAGE_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    getUsageByPrisonCodeSpec(
      request = UsageByPrisonCodeRequest(
        typeSubTypes = setOf(TypeSubTypeRequest("T1")),
        prisonCodes = setOf("NEO"),
      ),
      roles = listOf("ANY_OTHER_ROLE"),
    ).expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("invalidPrisonCodeUsageRequest")
  fun `400 bad request - invalid request for usage request by prison code`(
    request: UsageByPrisonCodeRequest,
    error: ErrorResponse,
  ) {
    val res = getUsageByPrisonCodeSpec(request).expectStatus().isBadRequest.errorResponse(HttpStatus.BAD_REQUEST)
    with(res) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo(error.developerMessage)
    }
  }

  @Test
  fun `200 ok - can find counts of type and subtype for multiple locations`() {
    val location1 = "LCO"
    val location2 = "LCT"
    val types = getAllTypes()
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .toList()
    val caseNotes = (0..40).flatMap {
      val type = types.random()
      listOf(
        givenCaseNote(generateCaseNote(type = type, locationId = location1)),
        givenCaseNote(generateCaseNote(type = type, locationId = location2)),
        givenCaseNote(generateCaseNote(type = type, locationId = "SME")),
      )
    }

    val toFind = caseNotes.random().let { Pair(it.subType.typeCode, it.subType.code) }

    val response = getUsageByPrisonCode(
      UsageByPrisonCodeRequest(
        prisonCodes = setOf(location1, location2),
        typeSubTypes = setOf(TypeSubTypeRequest(toFind.first, setOf(toFind.second))),
      ),
    )

    assertThat(response.content).hasSize(2)
    assertThat(response.content[location1]!!.first().count).isEqualTo(
      caseNotes.count { it.locationId == location1 && it.subType.typeCode == toFind.first && it.subType.code == toFind.second },
    )
    assertThat(response.content[location2]!!.first().count).isEqualTo(
      caseNotes.count { it.locationId == location2 && it.subType.typeCode == toFind.first && it.subType.code == toFind.second },
    )
  }

  @Test
  fun `200 ok - can find counts of multiple types and subtypes`() {
    val location = "LCM"
    val types = getAllTypes()
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .toList()
    val caseNotes = (0..10).map { givenCaseNote(generateCaseNote(type = types.random(), locationId = location)) }

    val toFind = caseNotes.map { Pair(it.subType.typeCode, it.subType.code) }.toSet().take(2)

    val response = getUsageByPrisonCode(
      UsageByPrisonCodeRequest(
        prisonCodes = setOf(location),
        typeSubTypes = toFind.map { TypeSubTypeRequest(it.first, setOf(it.second)) }.toSet(),
      ),
    )

    assertThat(response.content).hasSize(1)
    with(response.content[location]!!) {
      assertThat(size).isEqualTo(2)
      forEach { usage ->
        val matching = caseNotes.filter { it.subType.typeCode == usage.type && it.subType.code == usage.subType }
        assertThat(usage.count).isEqualTo(matching.size)
        assertThat(usage.latestNote).isEqualTo(LatestNote(matching.maxBy { it.occurredAt }.occurredAt))
      }
    }
  }

  @Test
  fun `can find by occurred at`() {
    val prisonCode = "OCC"
    val subType = givenRandomType()
    givenCaseNote(
      generateCaseNote(
        type = subType,
        occurredAt = LocalDateTime.now().minusDays(7),
        locationId = prisonCode,
      ),
    )
    val caseNote =
      givenCaseNote(
        generateCaseNote(
          type = subType,
          occurredAt = LocalDateTime.now().minusDays(5),
          locationId = prisonCode,
        ),
      )
    givenCaseNote(
      generateCaseNote(
        type = subType,
        occurredAt = LocalDateTime.now().minusDays(3),
        locationId = prisonCode,
      ),
    )

    val all = getUsageByPrisonCode(
      UsageByPrisonCodeRequest(
        prisonCodes = setOf(prisonCode),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
      ),
    )
    assertThat(all.content[prisonCode]!!.first().count).isEqualTo(3)

    val response = getUsageByPrisonCode(
      UsageByPrisonCodeRequest(
        prisonCodes = setOf(prisonCode),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
        occurredFrom = LocalDateTime.now().minusDays(6),
        occurredTo = LocalDateTime.now().minusDays(4),
      ),
    )

    assertThat(response.content[prisonCode]!!.first().count).isEqualTo(1)
    assertThat(response.content[prisonCode]!!.first().latestNote).isEqualTo(LatestNote(caseNote.occurredAt))
  }

  private fun getUsageByPrisonCodeSpec(
    request: UsageByPrisonCodeRequest = UsageByPrisonCodeRequest(),
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ) = webTestClient.post().uri(USAGE_URL)
    .bodyValue(request)
    .headers(addBearerAuthorisation(username, roles))
    .exchange()

  private fun getUsageByPrisonCode(
    request: UsageByPrisonCodeRequest = UsageByPrisonCodeRequest(),
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ): NoteUsageResponse<UsageByPrisonCodeResponse> = getUsageByPrisonCodeSpec(request, roles, username)
    .expectStatus().isOk
    .expectBody(object : ParameterizedTypeReference<NoteUsageResponse<UsageByPrisonCodeResponse>>() {})
    .returnResult().responseBody!!

  companion object {
    private const val USAGE_URL = "/case-notes/prison-usage"

    @JvmStatic
    fun invalidPrisonCodeUsageRequest() = listOf(
      of(
        UsageByPrisonCodeRequest(prisonCodes = setOf("ABC")),
        ErrorResponse(400, developerMessage = "400 BAD_REQUEST Validation failure: At least one type is required"),
      ),
      of(
        UsageByPrisonCodeRequest(typeSubTypes = setOf(TypeSubTypeRequest("T1", setOf()))),
        ErrorResponse(
          400,
          developerMessage = "400 BAD_REQUEST Validation failure: At least one prison code is required",
        ),
      ),
    )
  }
}
