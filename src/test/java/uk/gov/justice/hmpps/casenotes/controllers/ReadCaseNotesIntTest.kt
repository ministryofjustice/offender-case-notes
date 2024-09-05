package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.prisonNumber
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.LocalDateTime

class ReadCaseNotesIntTest : ResourceTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(urlToTest(prisonNumber())).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    getCaseNotes(prisonNumber(), roles = listOf("ANY_OTHER_ROLE")).expectStatus().isForbidden
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_CASE_NOTES_READ, ROLE_CASE_NOTES_WRITE])
  fun `can read a case note by id with appropriate role`(role: String) {
    val caseNote = givenCaseNote(generateCaseNote().withAmendment())
    val response = getCaseNotes(caseNote.prisonNumber, roles = listOf(role)).page()

    assertThat(response.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
    first.amendments.first().verifyAgainst(caseNote.amendments().first())
  }

  @Test
  fun `can filter case notes by author name`() {
    val prisonNumber = prisonNumber()
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber, authorUsername = "AuCn2"))
    givenCaseNote(generateCaseNote(prisonNumber, authorUsername = "AuCn1"))
    assertThat(getCaseNotes(prisonNumber).page().totalElements).isEqualTo(2)

    val response = getCaseNotes(prisonNumber, mapOf("authorUsername" to "AUCN2")).page()
    assertThat(response.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
  }

  @Test
  fun `can filter case notes by location id`() {
    val prisonNumber = prisonNumber()
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber, locationId = "SWI"))
    givenCaseNote(generateCaseNote(prisonNumber, locationId = "LEI"))
    assertThat(getCaseNotes(prisonNumber).page().totalElements).isEqualTo(2)

    val response = getCaseNotes(prisonNumber, mapOf("locationId" to "Swi")).page()
    assertThat(response.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
  }

  @Test
  fun `can filter sensitive case notes`() {
    val prisonNumber = prisonNumber()
    val sensitiveType = givenRandomType(sensitive = true)
    val nonSensitiveType = givenRandomType(sensitive = false)
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber, nonSensitiveType))
    givenCaseNote(generateCaseNote(prisonNumber, sensitiveType))
    assertThat(getCaseNotes(prisonNumber).page().totalElements).isEqualTo(2)

    val response = getCaseNotes(prisonNumber, mapOf("includeSensitive" to "false")).page()
    assertThat(response.totalElements).isEqualTo(1)
    val first = response.content.first()
    first.verifyAgainst(caseNote)
  }

  @Test
  fun `can filter by occurrence date time`() {
    val prisonNumber = prisonNumber()
    givenCaseNote(generateCaseNote(prisonNumber, occurredAt = LocalDateTime.now().minusDays(7)))
    val caseNote = givenCaseNote(generateCaseNote(prisonNumber, occurredAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(prisonNumber, occurredAt = LocalDateTime.now().minusDays(3)))
    assertThat(getCaseNotes(prisonNumber).page().totalElements).isEqualTo(3)

    val response = getCaseNotes(
      prisonNumber,
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
    val prisonNumber = prisonNumber()
    givenCaseNote(generateCaseNote(prisonNumber, text = "SEVEN", occurredAt = LocalDateTime.now().minusDays(7)))
    givenCaseNote(generateCaseNote(prisonNumber, text = "FIVE", occurredAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(prisonNumber, text = "THREE", occurredAt = LocalDateTime.now().minusDays(3)))

    val response1 = getCaseNotes(prisonNumber, mapOf("sort" to "occurrenceDateTime,asc")).page()
    assertThat(response1.totalElements).isEqualTo(3)
    assertThat(response1.content.map { it.text }).containsExactly("SEVEN", "FIVE", "THREE")

    val response2 = getCaseNotes(prisonNumber, mapOf("sort" to "occurrenceDateTime,desc")).page()
    assertThat(response2.totalElements).isEqualTo(3)
    assertThat(response2.content.map { it.text }).containsExactly("THREE", "FIVE", "SEVEN")
  }

  @Test
  fun `can sort by created at`() {
    val prisonNumber = prisonNumber()
    givenCaseNote(generateCaseNote(prisonNumber, text = "SEVEN", createdAt = LocalDateTime.now().minusDays(7)))
    givenCaseNote(generateCaseNote(prisonNumber, text = "FIVE", createdAt = LocalDateTime.now().minusDays(5)))
    givenCaseNote(generateCaseNote(prisonNumber, text = "THREE", createdAt = LocalDateTime.now().minusDays(3)))

    val response1 = getCaseNotes(prisonNumber, mapOf("sort" to "creationDateTime,asc")).page()
    assertThat(response1.totalElements).isEqualTo(3)
    assertThat(response1.content.map { it.text }).containsExactly("SEVEN", "FIVE", "THREE")

    val response2 = getCaseNotes(prisonNumber, mapOf("sort" to "creationDateTime,desc")).page()
    assertThat(response2.totalElements).isEqualTo(3)
    assertThat(response2.content.map { it.text }).containsExactly("THREE", "FIVE", "SEVEN")
  }

  @Test
  fun `can filter by parent type`() {
    val prisonNumber = prisonNumber()
    val types = getAllTypes().asSequence()
      .filter { !it.sensitive }
      .groupBy { it.parent.code }
      .map { it.value.take(2) }.flatten().take(20)
      .shuffled().toList()
    val caseNotes = types.map { givenCaseNote(generateCaseNote(prisonNumber, it)) }

    val parent = types.random().parent
    val toFind = caseNotes.filter { it.type.parent.code == parent.code }
    val response = getCaseNotes(prisonNumber, mapOf("type" to parent.code)).page()

    assertThat(response.totalElements.toInt()).isEqualTo(toFind.size)
    assertThat(response.content.map { it.type to it.subType })
      .containsExactlyInAnyOrderElementsOf(toFind.map { it.type.parent.code to it.type.code })
  }

  @Test
  fun `can filter by a single sub type`() {
    val prisonNumber = prisonNumber()
    val types = getAllTypes().asSequence()
      .filter { !it.sensitive }
      .groupBy { it.parent.code }
      .map { it.value.take(2) }.flatten().take(20)
      .toList()
    val caseNotes = types.map { givenCaseNote(generateCaseNote(prisonNumber, it)) }

    val toFind = caseNotes.random()

    val response = getCaseNotes(
      prisonNumber,
      mapOf("type" to toFind.type.parent.code, "subType" to toFind.type.code),
    ).page()

    assertThat(response.totalElements).isEqualTo(1)
    val found = response.content.first()
    assertThat(found.type).isEqualTo(toFind.type.parent.code)
    assertThat(found.typeDescription).isEqualTo(toFind.type.parent.description)
    assertThat(found.subType).isEqualTo(toFind.type.code)
    assertThat(found.subTypeDescription).isEqualTo(toFind.type.description)
  }

  private fun getCaseNotes(
    prisonNumber: String,
    queryParams: Map<String, String> = mapOf(),
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ) = webTestClient.get().uri { ub ->
    ub.path(urlToTest(prisonNumber))
    queryParams.forEach {
      ub.queryParam(it.key, it.value)
    }
    ub.build()
  }.headers(addBearerAuthorisation(username, roles))
    .header(CASELOAD_ID, ACTIVE_PRISON)
    .exchange()

  private fun urlToTest(prisonNumber: String) = "/case-notes/$prisonNumber"

  private fun WebTestClient.ResponseSpec.page(): TestResponsePage =
    expectBody(TestResponsePage::class.java).returnResult().responseBody!!
}

internal data class TestResponsePage(val content: MutableList<CaseNote>, val totalElements: Long)
