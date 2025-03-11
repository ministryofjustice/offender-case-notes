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
import uk.gov.justice.hmpps.casenotes.notes.NoteUsageRequest.DateType
import uk.gov.justice.hmpps.casenotes.notes.NoteUsageResponse
import uk.gov.justice.hmpps.casenotes.notes.TypeSubTypeRequest
import uk.gov.justice.hmpps.casenotes.notes.UsageByPersonIdentifierRequest
import uk.gov.justice.hmpps.casenotes.notes.UsageByPersonIdentifierResponse
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class NoteUsageByPersonIdentifiersIntTest : IntegrationTest() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(USAGE_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    getUsageByPersonIdentifiersSpec(
      request = UsageByPersonIdentifierRequest(
        typeSubTypes = setOf(TypeSubTypeRequest("T1")),
        personIdentifiers = setOf(personIdentifier()),
      ),
      roles = listOf("ANY_OTHER_ROLE"),
    ).expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("invalidPiUsageRequest")
  fun `400 bad request - invalid request for usage request by person identifiers`(
    request: UsageByPersonIdentifierRequest,
    error: ErrorResponse,
  ) {
    val res = getUsageByPersonIdentifiersSpec(request).expectStatus().isBadRequest.errorResponse(HttpStatus.BAD_REQUEST)
    with(res) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo(error.developerMessage)
    }
  }

  @Test
  fun `200 ok - can find counts of type and subtype for multiple person identifiers`() {
    val pi1 = personIdentifier()
    val pi2 = personIdentifier()
    val types = getAllTypes()
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .toList()
    val caseNotes = (0..40).flatMap {
      val type = types.random()
      listOf(
        givenCaseNote(generateCaseNote(pi1, type)),
        givenCaseNote(generateCaseNote(pi2, type)),
      )
    }

    val toFind = caseNotes.random().let { Pair(it.subType.typeCode, it.subType.code) }

    val response = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(pi1, pi2),
        typeSubTypes = setOf(TypeSubTypeRequest(toFind.first, setOf(toFind.second))),
      ),
    )

    assertThat(response.content).hasSize(2)
    assertThat(response.content[pi1]!!.first().count).isEqualTo(
      caseNotes.count { it.personIdentifier == pi1 && it.subType.typeCode == toFind.first && it.subType.code == toFind.second },
    )
    assertThat(response.content[pi2]!!.first().count).isEqualTo(
      caseNotes.count { it.personIdentifier == pi2 && it.subType.typeCode == toFind.first && it.subType.code == toFind.second },
    )
  }

  @Test
  fun `200 ok - can find counts of multiple types and subtypes`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes()
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .toList()
    val caseNotes = (0..10).map { givenCaseNote(generateCaseNote(personIdentifier, types.random())) }

    val toFind = caseNotes.map { Pair(it.subType.typeCode, it.subType.code) }.toSet().take(2)

    val response = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = toFind.map { TypeSubTypeRequest(it.first, setOf(it.second)) }.toSet(),
      ),
    )

    assertThat(response.content).hasSize(1)
    with(response.content[personIdentifier]!!) {
      assertThat(size).isEqualTo(2)
      forEach { usage ->
        val matching = caseNotes.filter { it.subType.typeCode == usage.type && it.subType.code == usage.subType }
        assertThat(usage.count).isEqualTo(matching.size)
        assertThat(usage.latestNote).isEqualTo(LatestNote(matching.maxBy { it.occurredAt }.occurredAt))
      }
    }
  }

  @Test
  fun `200 ok - can find counts of multiple types deducing subtypes`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes()
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .toList()
    val caseNotes = (0..10).map { givenCaseNote(generateCaseNote(personIdentifier, types.random())) }

    val toFind = caseNotes.map { it.subType.typeCode }.toSet().take(2)

    val response = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = toFind.map { TypeSubTypeRequest(it, setOf()) }.toSet(),
      ),
    )

    assertThat(response.content).hasSize(1)
    with(response.content[personIdentifier]!!) {
      assertThat(size).isEqualTo(
        caseNotes.filter { it.subType.typeCode in toFind }
          .distinctBy { Pair(it.subType.typeCode, it.subType.code) }.size,
      )
      forEach { usage ->
        val matching = caseNotes.filter { it.subType.typeCode == usage.type && it.subType.code == usage.subType }
        assertThat(usage.count).isEqualTo(matching.size)
        assertThat(usage.latestNote).isEqualTo(LatestNote(matching.maxBy { it.occurredAt }.occurredAt))
      }
    }
  }

  @Test
  fun `can find by occurred at`() {
    val personIdentifier = personIdentifier()
    val subType = givenRandomType()
    givenCaseNote(generateCaseNote(personIdentifier, subType, occurredAt = LocalDateTime.now().minusDays(7)))
    val caseNote =
      givenCaseNote(generateCaseNote(personIdentifier, subType, occurredAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(personIdentifier, subType, occurredAt = LocalDateTime.now().minusDays(3)))

    val all = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
        dateType = DateType.OCCURRED_AT,
      ),
    )
    assertThat(all.content[personIdentifier]!!.first().count).isEqualTo(3)

    val response = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
        occurredFrom = LocalDateTime.now().minusDays(6),
        occurredTo = LocalDateTime.now().minusDays(4),
      ),
    )

    assertThat(response.content[personIdentifier]!!.first().count).isEqualTo(1)
    assertThat(response.content[personIdentifier]!!.first().latestNote).isEqualTo(LatestNote(caseNote.occurredAt))
  }

  @Test
  fun `can find by created at`() {
    val personIdentifier = personIdentifier()
    val subType = givenRandomType()
    givenCaseNote(generateCaseNote(personIdentifier, subType, createdAt = LocalDateTime.now().minusDays(7)))
    val caseNote =
      givenCaseNote(generateCaseNote(personIdentifier, subType, createdAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(personIdentifier, subType, createdAt = LocalDateTime.now().minusDays(3)))

    val all = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
        dateType = DateType.CREATED_AT,
      ),
    )
    assertThat(all.content[personIdentifier]!!.first().count).isEqualTo(3)

    val response = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
        occurredFrom = LocalDateTime.now().minusDays(6),
        occurredTo = LocalDateTime.now().minusDays(4),
        dateType = DateType.CREATED_AT,
      ),
    )

    assertThat(response.content[personIdentifier]!!.first().count).isEqualTo(1)
    assertThat(response.content[personIdentifier]!!.first().latestNote)
      .isEqualTo(LatestNote(caseNote.createdAt.truncatedTo(SECONDS)))
  }

  @Test
  fun `can filter by author`() {
    val personIdentifier = personIdentifier()
    val subType = givenRandomType()
    givenCaseNote(generateCaseNote(personIdentifier, subType, authorUserId = "12345"))
    givenCaseNote(generateCaseNote(personIdentifier, subType, authorUserId = "23456"))
    givenCaseNote(generateCaseNote(personIdentifier, subType, authorUserId = "12345"))

    val all = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
      ),
    )
    assertThat(all.content[personIdentifier]!!.first().count).isEqualTo(3)

    val response1 = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
        authorIds = setOf("23456"),
      ),
    )
    assertThat(response1.content[personIdentifier]!!.first().count).isEqualTo(1)

    val response2 = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
        authorIds = setOf("12345"),
      ),
    )
    assertThat(response2.content[personIdentifier]!!.first().count).isEqualTo(2)
  }

  @Test
  fun `can filter by prison code`() {
    val personIdentifier = personIdentifier()
    val subType = givenRandomType()
    givenCaseNote(generateCaseNote(personIdentifier, subType, locationId = "LOC1"))
    givenCaseNote(generateCaseNote(personIdentifier, subType, locationId = "LOC2"))
    givenCaseNote(generateCaseNote(personIdentifier, subType, locationId = "LOC2"))

    val all = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
      ),
    )
    assertThat(all.content[personIdentifier]!!.first().count).isEqualTo(3)

    val response1 = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
        prisonCode = "LOC1",
      ),
    )
    assertThat(response1.content[personIdentifier]!!.first().count).isEqualTo(1)

    val response2 = getUsageByPersonIdentifiers(
      UsageByPersonIdentifierRequest(
        personIdentifiers = setOf(personIdentifier),
        typeSubTypes = setOf(TypeSubTypeRequest(subType.typeCode, setOf(subType.code))),
        prisonCode = "LOC2",
      ),
    )
    assertThat(response2.content[personIdentifier]!!.first().count).isEqualTo(2)
  }

  private fun getUsageByPersonIdentifiersSpec(
    request: UsageByPersonIdentifierRequest = UsageByPersonIdentifierRequest(dateType = DateType.OCCURRED_AT),
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ) = webTestClient.post().uri(USAGE_URL)
    .bodyValue(request)
    .headers(addBearerAuthorisation(username, roles))
    .exchange()

  private fun getUsageByPersonIdentifiers(
    request: UsageByPersonIdentifierRequest = UsageByPersonIdentifierRequest(),
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ): NoteUsageResponse<UsageByPersonIdentifierResponse> = getUsageByPersonIdentifiersSpec(request, roles, username)
    .expectStatus().isOk
    .expectBody(object : ParameterizedTypeReference<NoteUsageResponse<UsageByPersonIdentifierResponse>>() {})
    .returnResult().responseBody!!

  companion object {
    private const val USAGE_URL = "/case-notes/usage"

    @JvmStatic
    fun invalidPiUsageRequest() = listOf(
      of(
        UsageByPersonIdentifierRequest(personIdentifiers = setOf(personIdentifier())),
        ErrorResponse(400, developerMessage = "400 BAD_REQUEST Validation failure: At least one type is required"),
      ),
      of(
        UsageByPersonIdentifierRequest(typeSubTypes = setOf(TypeSubTypeRequest("T1", setOf()))),
        ErrorResponse(
          400,
          developerMessage = "400 BAD_REQUEST Validation failure: At least one person identifier is required",
        ),
      ),
    )
  }
}
