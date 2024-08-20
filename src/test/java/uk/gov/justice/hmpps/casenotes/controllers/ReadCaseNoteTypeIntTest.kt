package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.hmpps.casenotes.types.ActiveYn
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType
import uk.gov.justice.hmpps.casenotes.types.SelectableBy.DPS_USER
import uk.gov.justice.hmpps.casenotes.types.TypeInclude.INACTIVE
import uk.gov.justice.hmpps.casenotes.types.TypeInclude.RESTRICTED
import uk.gov.justice.hmpps.casenotes.types.TypeInclude.SENSITIVE
import uk.gov.justice.hmpps.casenotes.utils.ROLE_ADD_SENSITIVE_CASE_NOTES
import uk.gov.justice.hmpps.casenotes.utils.ROLE_POM

class ReadCaseNoteTypeIntTest : ResourceTest() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(BASE_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `default request provides inactive, active, non secure and non restricted types`() {
    val types = getCaseNoteTypes(
      "API_TEST_USER",
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isNotEmpty()
    assertThat(types.sensitiveOrRestricted()).isEmpty()
    assertThat(types.filter { it.code == "READ_TEST" }).isEmpty()
    assertThat(types.filter { it.code == "NOT_DPS" }).isNotEmpty()
  }

  @Test
  fun `can request only active types`() {
    val types = getCaseNoteTypes(
      "API_TEST_USER",
      requestParams = mapOf("include" to listOf()),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isEmpty()
    assertThat(types.sensitiveOrRestricted()).isEmpty()
    assertThat(types.filter { it.code == "READ_TEST" }).isEmpty()
  }

  @Test
  fun `can request sensitive types`() {
    val types = getCaseNoteTypes(
      "API_TEST_USER",
      requestParams = mapOf("include" to listOf(SENSITIVE.name)),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isEmpty()
    assertThat(types.sensitive()).isNotEmpty()
    assertThat(types.restricted()).isEmpty()
    val parent = types.first { it.code == "READ_TEST" }
    assertThat(parent.subCodes).hasSize(1)
    with(parent.subCodes.first()) {
      assertThat(code).isEqualTo("ACT_SEN")
      assertThat(active).isTrue()
      assertThat(sensitive).isTrue()
      assertThat(restrictedUse).isFalse()
    }
  }

  @Test
  fun `can request restricted types`() {
    val types = getCaseNoteTypes(
      "API_TEST_USER",
      requestParams = mapOf("include" to listOf(RESTRICTED.name)),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isEmpty()
    assertThat(types.sensitive()).isEmpty()
    assertThat(types.restricted()).isNotEmpty()
    val parent = types.first { it.code == "READ_TEST" }
    assertThat(parent.subCodes).hasSize(1)
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
      username = "API_TEST_USER",
      requestParams = mapOf("include" to listOf(), "selectableBy" to listOf(DPS_USER.name)),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isEmpty()
    assertThat(types.sensitiveOrRestricted()).isEmpty()
    assertThat(types.notDpsUserSelectable()).isEmpty()
    assertThat(types.filter { it.code == "NOT_DPS" }).isEmpty()
  }

  @Test
  fun `types have consistent order`() {
    val types = getCaseNoteTypes(
      username = "API_TEST_USER",
      requestParams = mapOf("include" to listOf(INACTIVE.name)),
    ).successList<CaseNoteType>()
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

  @ParameterizedTest
  @ValueSource(strings = [ROLE_ADD_SENSITIVE_CASE_NOTES, ROLE_POM])
  fun `with appropriate role - default is to view sensitive and restricted`(role: String) {
    val types = getCaseNoteTypes(
      "POM_TEST_USER",
      listOf("ROLE_$role"),
      mapOf("include" to listOf()),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.sensitive()).isNotEmpty()
    assertThat(types.restricted()).isNotEmpty()
    assertThat(types.notDpsUserSelectable()).isNotEmpty()
    val parent = types.first { it.code == "READ_TEST" }
    assertThat(parent.subCodes).hasSize(2)
  }

  private fun List<CaseNoteType>.inactive() = flatMap { it.subCodes }.filter { !it.active }
  private fun List<CaseNoteType>.withoutSubTypes() = filter { it.subCodes.isEmpty() }
  private fun List<CaseNoteType>.sensitiveOrRestricted() =
    flatMap { it.subCodes }.filter { it.sensitive || it.restrictedUse }

  private fun List<CaseNoteType>.sensitive() =
    flatMap { it.subCodes }.filter { it.sensitive }

  private fun List<CaseNoteType>.restricted() =
    flatMap { it.subCodes }.filter { it.restrictedUse }

  private fun List<CaseNoteType>.notDpsUserSelectable() =
    flatMap { it.subCodes }.filter { DPS_USER !in it.selectableBy }

  private fun getCaseNoteTypes(
    username: String,
    roles: List<String> = listOf(),
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
