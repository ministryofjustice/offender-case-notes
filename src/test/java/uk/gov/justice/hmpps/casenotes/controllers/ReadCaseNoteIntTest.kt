package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.SOURCE_AUTO
import uk.gov.justice.hmpps.casenotes.notes.SOURCE_INST
import uk.gov.justice.hmpps.casenotes.notes.SOURCE_OCNS
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.util.UUID
import java.util.function.Predicate

class ReadCaseNoteIntTest : ResourceTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(urlToTest(personIdentifier(), UUID.randomUUID().toString()))
      .exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    getCaseNote(
      personIdentifier(),
      UUID.randomUUID().toString(),
      listOf("ANY_OTHER_ROLE"),
    ).expectStatus().isForbidden
  }

  @ParameterizedTest
  @ValueSource(strings = [SecurityUserContext.ROLE_CASE_NOTES_READ, SecurityUserContext.ROLE_CASE_NOTES_WRITE])
  fun `can read a case note by id with appropriate role`(role: String) {
    val caseNote = givenCaseNote(generateCaseNote().withAmendment())
    val response = getCaseNote(caseNote.personIdentifier, caseNote.id.toString(), listOf(role))
      .success<CaseNote>()

    response.verifyAgainst(caseNote)
    response.amendments.first().verifyAgainst(caseNote.amendments().first())
  }

  @Test
  fun `can read a case note by legacy id`() {
    val caseNote = givenCaseNote(generateCaseNote(legacyId = NomisIdGenerator.newId()).withAmendment())
    val response = getCaseNote(caseNote.personIdentifier, caseNote.legacyId.toString())
      .success<CaseNote>()

    response.verifyAgainst(caseNote)
    response.amendments.first().verifyAgainst(caseNote.amendments().first())
  }

  @ParameterizedTest
  @MethodSource("caseNotesWithSource")
  fun `correctly calculates the source`(filter: Predicate<SubType>, source: String) {
    val personIdentifier = personIdentifier()
    val type = getAllTypes().first(filter::test)
    val cn = givenCaseNote(generateCaseNote(personIdentifier, type))

    val response = getCaseNote(cn.personIdentifier, cn.id.toString()).success<CaseNote>()
    assertThat(response.source).isEqualTo(source)
  }

  private fun getCaseNote(
    prisonNumber: String,
    caseNoteId: String,
    roles: List<String> = listOf(SecurityUserContext.ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
  ) = webTestClient.get().uri(urlToTest(prisonNumber, caseNoteId))
    .headers(addBearerAuthorisation(username, roles))
    .header(CASELOAD_ID, ACTIVE_PRISON)
    .exchange()

  private fun urlToTest(prisonNumber: String, caseNoteId: String) = "/case-notes/$prisonNumber/$caseNoteId"

  companion object {
    @JvmStatic
    fun caseNotesWithSource() = listOf(
      Arguments.of(Predicate<SubType> { !it.syncToNomis }, SOURCE_OCNS),
      Arguments.of(Predicate<SubType> { it.syncToNomis && !it.dpsUserSelectable }, SOURCE_AUTO),
      Arguments.of(Predicate<SubType> { it.syncToNomis && it.dpsUserSelectable }, SOURCE_INST),
    )
  }
}
