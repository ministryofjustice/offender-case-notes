package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType
import uk.gov.justice.hmpps.casenotes.types.SelectableBy.DPS_USER

class ReadTypeIntTest : IntegrationTest() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(BASE_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    getCaseNoteTypes(roles = listOf("ANY_OTHER_ROLE")).expectStatus().isForbidden
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_CASE_NOTES_READ, ROLE_CASE_NOTES_WRITE])
  fun `read and write case note roles can read types`(role: String) {
    val types = getCaseNoteTypes(roles = listOf(role)).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
  }

  @Test
  fun `default request provides active and non restricted types`() {
    val types = getCaseNoteTypes().successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isEmpty()
    assertThat(types.restricted()).isEmpty()
    assertThat(types.filter { it.code == "READ_TEST" }).isNotEmpty()
    assertThat(types.filter { it.code == "NOT_DPS" }).isNotEmpty()
  }

  @Test
  fun `can request only active types`() {
    val types = getCaseNoteTypes(
      requestParams = mapOf("includeInactive" to listOf("false"), "includeRestricted" to listOf("false")),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isEmpty()
    assertThat(types.restricted()).isEmpty()
    val parent = types.first { it.code == "READ_TEST" }
    assertThat(parent.subCodes).hasSize(1)
  }

  @Test
  fun `can request restricted types`() {
    val types = getCaseNoteTypes(
      requestParams = mapOf("includeRestricted" to listOf("true"), "includeInactive" to listOf("false")),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isEmpty()
    assertThat(types.restricted()).isNotEmpty()
    val parent = types.first { it.code == "READ_TEST" }
    assertThat(parent.subCodes).hasSize(2)
    with(parent.subCodes.first()) {
      assertThat(code).isEqualTo("ACT_RES")
      assertThat(active).isTrue()
      assertThat(sensitive).isFalse()
      assertThat(restrictedUse).isTrue()
    }
  }

  @Test
  fun `can filter types to only those selectable by dps user`() {
    val types = getCaseNoteTypes(
      requestParams = mapOf(
        "includeInactive" to listOf("false"),
        "includeRestricted" to listOf("false"),
        "selectableBy" to listOf(DPS_USER.name),
      ),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isEmpty()
    assertThat(types.restricted()).isEmpty()
    assertThat(types.notDpsUserSelectable()).isEmpty()
    assertThat(types.filter { it.code == "NOT_DPS" }).isEmpty()
  }

  @Test
  fun `types have consistent order`() {
    val types = getCaseNoteTypes(requestParams = mapOf("includeInactive" to listOf("true")))
      .successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    val parent = types.first { it.code == "LTR" }
    assertThat(parent.subCodes.map { it.code }).containsExactly(
      "FO",
      "LTRFO",
      "FTP",
      "LTRFTP",
      "LTRTO",
      "TO",
      "LTRTTP",
      "TTP",
    )
  }

  private fun List<CaseNoteType>.inactive() = flatMap { it.subCodes }.filter { !it.active }
  private fun List<CaseNoteType>.withoutSubTypes() = filter { it.subCodes.isEmpty() }

  private fun List<CaseNoteType>.restricted() =
    flatMap { it.subCodes }.filter { it.restrictedUse }

  private fun List<CaseNoteType>.notDpsUserSelectable() =
    flatMap { it.subCodes }.filter { DPS_USER !in it.selectableBy }

  private fun getCaseNoteTypes(
    username: String = "API_TEST_USER",
    roles: List<String> = listOf(ROLE_CASE_NOTES_READ),
    requestParams: Map<String, List<String>> = mapOf(),
  ) = webTestClient.get().uri { builder ->
    builder.path(BASE_URL)
    requestParams.forEach {
      builder.queryParam(it.key, it.value)
    }
    builder.build()
  }.headers(addBearerAuthorisation(username, roles))
    .exchange()

  companion object {
    const val BASE_URL = "/case-notes/types"
  }
}
