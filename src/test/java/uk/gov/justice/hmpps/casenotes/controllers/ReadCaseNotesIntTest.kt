package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.hmpps.casenotes.config.CaseloadIdHeader
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.LocalDateTime

class ReadCaseNotesIntTest : IntegrationTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(urlToTest(personIdentifier())).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    getCaseNotes(personIdentifier(), roles = listOf("ANY_OTHER_ROLE")).expectStatus().isForbidden
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_CASE_NOTES_READ, ROLE_CASE_NOTES_WRITE])
  fun `can read a case note by id with appropriate role`(role: String) {
    val caseNote = givenCaseNote(generateCaseNote().withAmendment())
    val response = getCaseNotes(caseNote.personIdentifier, roles = listOf(role)).page()

    assertThat(response.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
    first.amendments.first().verifyAgainst(caseNote.amendments().first())
  }

  @Test
  fun `can filter case notes by author name`() {
    val personIdentifier = personIdentifier()
    val authorUsername = "AUCN2"
    val caseNote = givenCaseNote(generateCaseNote(personIdentifier, authorUsername = authorUsername))
    givenCaseNote(generateCaseNote(personIdentifier, authorUsername = "AuCn1"))
    assertThat(getCaseNotes(personIdentifier).page().totalElements).isEqualTo(2)

    val response = getCaseNotes(personIdentifier, mapOf("authorUsername" to authorUsername)).page()
    assertThat(response.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
  }

  @Test
  fun `can filter case notes by location id`() {
    val personIdentifier = personIdentifier()
    val locationId = "SWI"
    val caseNote = givenCaseNote(generateCaseNote(personIdentifier, locationId = locationId))
    givenCaseNote(generateCaseNote(personIdentifier, locationId = "LEI"))
    assertThat(getCaseNotes(personIdentifier).page().totalElements).isEqualTo(2)

    val response = getCaseNotes(personIdentifier, mapOf("locationId" to locationId)).page()
    assertThat(response.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
  }

  @Test
  fun `can filter sensitive case notes`() {
    val personIdentifier = personIdentifier()
    val sensitiveType = givenRandomType(sensitive = true)
    val nonSensitiveType = givenRandomType(sensitive = false)
    val caseNote = givenCaseNote(generateCaseNote(personIdentifier, nonSensitiveType))
    givenCaseNote(generateCaseNote(personIdentifier, sensitiveType))
    assertThat(getCaseNotes(personIdentifier).page().totalElements).isEqualTo(2)

    val response = getCaseNotes(personIdentifier, mapOf("includeSensitive" to "false")).page()
    assertThat(response.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
  }

  @Test
  fun `can filter by occurrence date time`() {
    val personIdentifier = personIdentifier()
    givenCaseNote(generateCaseNote(personIdentifier, occurredAt = LocalDateTime.now().minusDays(7)))
    val caseNote = givenCaseNote(generateCaseNote(personIdentifier, occurredAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(personIdentifier, occurredAt = LocalDateTime.now().minusDays(3)))
    assertThat(getCaseNotes(personIdentifier).page().totalElements).isEqualTo(3)

    val response = getCaseNotes(
      personIdentifier,
      mapOf(
        "startDate" to LocalDateTime.now().minusDays(6).toString(),
        "endDate" to LocalDateTime.now().minusDays(4).toString(),
      ),
    ).page()
    assertThat(response.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
  }

  @Test
  fun `can sort by occurred at`() {
    val personIdentifier = personIdentifier()
    givenCaseNote(generateCaseNote(personIdentifier, text = "SEVEN", occurredAt = LocalDateTime.now().minusDays(7)))
    givenCaseNote(generateCaseNote(personIdentifier, text = "FIVE", occurredAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(personIdentifier, text = "THREE", occurredAt = LocalDateTime.now().minusDays(3)))

    val response1 = getCaseNotes(personIdentifier, mapOf("sort" to "occurrenceDateTime,asc")).page()
    assertThat(response1.totalElements).isEqualTo(3)
    assertThat(response1.content.map { it.text }).containsExactly("SEVEN", "FIVE", "THREE")

    val response2 = getCaseNotes(personIdentifier, mapOf("sort" to "occurrenceDateTime,desc")).page()
    assertThat(response2.totalElements).isEqualTo(3)
    assertThat(response2.content.map { it.text }).containsExactly("THREE", "FIVE", "SEVEN")
  }

  @Test
  fun `can sort by created at`() {
    val personIdentifier = personIdentifier()
    givenCaseNote(generateCaseNote(personIdentifier, text = "SEVEN", createdAt = LocalDateTime.now().minusDays(7)))
    givenCaseNote(generateCaseNote(personIdentifier, text = "FIVE", createdAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(personIdentifier, text = "THREE", createdAt = LocalDateTime.now().minusDays(3)))

    val response1 = getCaseNotes(personIdentifier, mapOf("sort" to "creationDateTime,asc")).page()
    assertThat(response1.totalElements).isEqualTo(3)
    assertThat(response1.content.map { it.text }).containsExactly("SEVEN", "FIVE", "THREE")

    val response2 = getCaseNotes(personIdentifier, mapOf("sort" to "creationDateTime,desc")).page()
    assertThat(response2.totalElements).isEqualTo(3)
    assertThat(response2.content.map { it.text }).containsExactly("THREE", "FIVE", "SEVEN")
  }

  @Test
  fun `can filter by parent type`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes().asSequence()
      .filter { !it.sensitive }
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .shuffled().toList()
    val caseNotes = types.map { givenCaseNote(generateCaseNote(personIdentifier, it)) }

    val parent = types.random().type
    val toFind = caseNotes.filter { it.subType.type.code == parent.code }
    val response = getCaseNotes(personIdentifier, mapOf("type" to parent.code)).page()

    assertThat(response.totalElements.toInt()).isEqualTo(toFind.size)
    assertThat(response.content.map { it.type to it.subType })
      .containsExactlyInAnyOrderElementsOf(toFind.map { it.subType.type.code to it.subType.code })
  }

  @Test
  fun `can filter by a single sub type`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes().asSequence()
      .filter { !it.sensitive }
      .groupBy { it.type.code }
      .map { it.value.take(2) }.flatten().take(20)
      .toList()
    val caseNotes = types.map { givenCaseNote(generateCaseNote(personIdentifier, it)) }

    val toFind = caseNotes.random()

    val response = getCaseNotes(
      personIdentifier,
      mapOf("type" to toFind.subType.type.code, "subType" to toFind.subType.code),
    ).page()

    assertThat(response.totalElements).isEqualTo(1)
    val found = response.content.first()
    assertThat(found.type).isEqualTo(toFind.subType.type.code)
    assertThat(found.typeDescription).isEqualTo(toFind.subType.type.description)
    assertThat(found.subType).isEqualTo(toFind.subType.code)
    assertThat(found.subTypeDescription).isEqualTo(toFind.subType.description)
  }

  @Test
  fun `can retrieve paginated case notes with amendments`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes().asSequence().take(20)
    types.forEach { givenCaseNote(generateCaseNote(personIdentifier, it)).withAmendment().withAmendment().withAmendment() }

    val response = getCaseNotes(
      personIdentifier,
      mapOf("includeSensitive" to "true"),
    ).page()

    assertThat(response.totalElements).isEqualTo(20)
  }

  private fun getCaseNotes(
    personIdentifier: String,
    queryParams: Map<String, String> = mapOf(),
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ) = webTestClient.get().uri { ub ->
    ub.path(urlToTest(personIdentifier))
    queryParams.forEach {
      ub.queryParam(it.key, it.value)
    }
    ub.build()
  }.headers(addBearerAuthorisation(username, roles))
    .header(CaseloadIdHeader.NAME, ACTIVE_PRISON)
    .exchange()

  private fun urlToTest(personIdentifier: String) = "/case-notes/$personIdentifier"

  private fun WebTestClient.ResponseSpec.page(): TestResponsePage = expectBody(TestResponsePage::class.java).returnResult().responseBody!!
}

internal data class TestResponsePage(val content: MutableList<CaseNote>, val totalElements: Long)
