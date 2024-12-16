package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.sar.SarAmendment
import uk.gov.justice.hmpps.casenotes.sar.SarNote
import uk.gov.justice.hmpps.casenotes.sar.SubjectAccessResponse
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class SubjectAccessRequestIntTest : IntegrationTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(BASE_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    getSarContent(roles = listOf(SecurityUserContext.ROLE_CASE_NOTES_WRITE)).expectStatus().isForbidden
  }

  @Test
  fun `209 no content - service does not use crn to identify people`() {
    getSarContent(mapOf("crn" to "A123456")).expectStatus().isEqualTo(209)
  }

  @Test
  fun `204 no content - service does not hold any data for the person identifier`() {
    getSarContent(mapOf("prn" to "NONEXISTENT")).expectStatus().isNoContent
  }

  @Test
  fun `200 ok - service returns all non sync to nomis case notes for a person identifier when no dates provided`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes().filter { !it.syncToNomis }
    val cn = (1..100).map {
      val cn = generateCaseNote(personIdentifier, types.random())
      if (it % 5 == 0) {
        cn.withAmendment()
      }
      givenCaseNote(cn)
    }

    val res = getSarContent(mapOf("prn" to personIdentifier)).success<SubjectAccessResponse>()
    assertThat(res.content).hasSize(100)
    assertThat(res.content.flatMap { it.amendments }).hasSize(20)
    assertThat(
      res.content.map { it.type to it.subType }.toSet(),
    ).containsExactlyInAnyOrderElementsOf(cn.map { it.subType.type.description to it.subType.description }.toSet())
  }

  @Test
  fun `200 ok - service returns case notes created at or after from date`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes().filter { !it.syncToNomis }
    val from = LocalDateTime.now().minusDays(30)
    givenCaseNote(
      generateCaseNote(
        personIdentifier,
        types.random(),
        createdAt = from.minusDays(1),
        text = "created before",
      ),
    )
    val on = givenCaseNote(
      generateCaseNote(
        personIdentifier,
        types.random(),
        createdAt = from,
        text = "created on",
      ).withAmendment(),
    )
    val after = givenCaseNote(
      generateCaseNote(
        personIdentifier,
        types.random(),
        createdAt = from.plusDays(5),
        text = "created after",
      ),
    )

    val res = getSarContent(mapOf("prn" to personIdentifier, "fromDate" to from.format(DateTimeFormatter.ISO_DATE)))
      .success<SubjectAccessResponse>()
    assertThat(res.content).hasSize(2)
    assertThat(res.content.flatMap { it.amendments }).hasSize(1)

    res.content[0].verifyAgainst(after)
    res.content[1].verifyAgainst(on)
    res.content[1].amendments.first().verifyAgainst(on.amendments().first())
  }

  @Test
  fun `200 ok - service returns all case notes at or before to date`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes().filter { !it.syncToNomis }
    val to = LocalDateTime.now().minusDays(10)
    givenCaseNote(generateCaseNote(personIdentifier, types.random(), createdAt = to.plusDays(1), text = "created after"))
    val on = givenCaseNote(generateCaseNote(personIdentifier, types.random(), createdAt = to, text = "created on").withAmendment())
    val before = givenCaseNote(generateCaseNote(personIdentifier, types.random(), createdAt = to.minusDays(5), text = "created before"))

    val res = getSarContent(mapOf("prn" to personIdentifier, "toDate" to to.format(DateTimeFormatter.ISO_DATE)))
      .success<SubjectAccessResponse>()
    assertThat(res.content).hasSize(2)
    assertThat(res.content.flatMap { it.amendments }).hasSize(1)

    res.content[0].verifyAgainst(on)
    res.content[0].amendments.first().verifyAgainst(on.amendments().first())
    res.content[1].verifyAgainst(before)
  }

  @Test
  fun `200 ok - service returns all case notes between from and to inclusive`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes().filter { !it.syncToNomis }
    val from = LocalDateTime.now().minusDays(30)
    val to = LocalDateTime.now().minusDays(10)
    givenCaseNote(generateCaseNote(personIdentifier, types.random(), createdAt = from.minusDays(1), text = "created before from"))
    val onFrom = givenCaseNote(generateCaseNote(personIdentifier, types.random(), createdAt = from, text = "created on from"))
    val onTo = givenCaseNote(generateCaseNote(personIdentifier, types.random(), createdAt = to, text = "created on to"))
    val between = givenCaseNote(generateCaseNote(personIdentifier, types.random(), createdAt = from.plusDays(10), text = "between"))
    givenCaseNote(generateCaseNote(personIdentifier, types.random(), createdAt = to.plusDays(1), text = "created after to"))

    val res = getSarContent(
      mapOf(
        "prn" to personIdentifier,
        "fromDate" to from.format(DateTimeFormatter.ISO_DATE),
        "toDate" to to.format(DateTimeFormatter.ISO_DATE),
      ),
    ).success<SubjectAccessResponse>()

    assertThat(res.content).hasSize(3)
    res.content[0].verifyAgainst(onTo)
    res.content[1].verifyAgainst(between)
    res.content[2].verifyAgainst(onFrom)
  }

  private fun getSarContent(
    params: Map<String, String> = mapOf(),
    roles: List<String> = listOf("ROLE_SAR_DATA_ACCESS"),
  ) = webTestClient.get().uri { ub ->
    ub.path(BASE_URL)
    params.forEach {
      ub.queryParam(it.key, it.value)
    }
    ub.build()
  }.headers(addBearerAuthorisation("sar-api-client", roles)).exchange()

  companion object {
    const val BASE_URL = "/subject-access-request"
  }
}

private fun SarNote.verifyAgainst(note: Note) {
  assertThat(type).isEqualTo(note.subType.type.description)
  assertThat(subType).isEqualTo(note.subType.description)
  assertThat(authorUsername).matches(note.authorUsername)
  assertThat(creationDateTime.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(note.createdAt.truncatedTo(ChronoUnit.SECONDS))
  assertThat(text).isEqualTo(note.text)
}

private fun SarAmendment.verifyAgainst(amendment: Amendment) {
  assertThat(authorUsername).matches(amendment.authorUsername)
  assertThat(creationDateTime.truncatedTo(ChronoUnit.SECONDS))
    .isEqualTo(amendment.createdAt.truncatedTo(ChronoUnit.SECONDS))
  assertThat(additionalNoteText).isEqualTo(amendment.text)
}
