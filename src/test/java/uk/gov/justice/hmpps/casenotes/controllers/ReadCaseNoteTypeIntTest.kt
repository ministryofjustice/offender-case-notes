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
import uk.gov.justice.hmpps.casenotes.utils.ROLE_VIEW_SENSITIVE_CASE_NOTES

class ReadCaseNoteTypeIntTest : ResourceTest() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(BASE_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `no roles can only view non secure and non restricted types`() {
    val types = getCaseNoteTypes(
      "API_TEST_USER",
      requestParams = mapOf("include" to listOf(SENSITIVE.name, RESTRICTED.name)),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.sensitiveOrRestricted()).isEmpty()
    assertThat(types.filter { it.code == "READ_TEST" }).isEmpty()
  }

  @Test
  fun `client token without user id can only view appropriate types`() {
    val types = webTestClient.get().uri {
      it.path(BASE_URL).queryParam("include", listOf(SENSITIVE.name, RESTRICTED.name)).build()
    }
      .headers(addBearerToken(jwtHelper.createJwt("API_TEST_USER", userId = null)))
      .exchange().successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.sensitiveOrRestricted()).isEmpty()
    assertThat(types.filter { it.code == "READ_TEST" }).isEmpty()
  }

  @Test
  fun `with role view sensitive - can only view sensitive and not restricted`() {
    val types =
      getCaseNoteTypes(
        "ViewSensitive",
        listOf("ROLE_$ROLE_VIEW_SENSITIVE_CASE_NOTES"),
        mapOf("include" to listOf(SENSITIVE.name, RESTRICTED.name)),
      ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.restricted()).isEmpty()
    assertThat(types.sensitive()).isNotEmpty()
    val parent = types.first { it.code == "READ_TEST" }
    assertThat(parent.subCodes).hasSize(1)
    with(parent.subCodes.first()) {
      assertThat(code).isEqualTo("ACT_SEN")
      assertThat(activeFlag).isEqualTo(ActiveYn.Y)
      assertThat(sensitive).isTrue()
      assertThat(restrictedUse).isFalse()
    }
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_ADD_SENSITIVE_CASE_NOTES, ROLE_POM])
  fun `with appropriate role - can view sensitive and restricted`(role: String) {
    val types = getCaseNoteTypes(
      "SpecialPrivileges",
      listOf("ROLE_$role"),
      mapOf("include" to listOf(SENSITIVE.name, RESTRICTED.name)),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.sensitive()).isNotEmpty()
    assertThat(types.restricted()).isNotEmpty()
    assertThat(types.notDpsUserSelectable()).isNotEmpty()
    val parent = types.first { it.code == "READ_TEST" }
    assertThat(parent.subCodes).hasSize(2)
  }

  @Test
  fun `user can filter types to exclude inactive, sensitive and restricted`() {
    val types = getCaseNoteTypes(
      "AnyUser",
      listOf("ROLE_$ROLE_POM"),
      mapOf("include" to listOf()),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isEmpty()
    assertThat(types.sensitiveOrRestricted()).isEmpty()
    assertThat(types.filter { it.code == "READ_TEST" }).isEmpty()
  }

  @Test
  fun `user can filter types to include inactive`() {
    val types = getCaseNoteTypes(
      "AnyUser",
      listOf("ROLE_$ROLE_POM"),
      mapOf("include" to listOf(INACTIVE.name)),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isNotEmpty()
    assertThat(types.sensitiveOrRestricted()).isEmpty()
  }

  @Test
  fun `user can filter types to only those selectable by dps user`() {
    val types = getCaseNoteTypes(
      username = "AnyUser",
      roles = listOf("ROLE_$ROLE_POM"),
      requestParams = mapOf("include" to listOf(), "selectableBy" to listOf(DPS_USER.name)),
    ).successList<CaseNoteType>()
    assertThat(types.withoutSubTypes()).isEmpty()
    assertThat(types.inactive()).isEmpty()
    assertThat(types.sensitiveOrRestricted()).isEmpty()
    assertThat(types.notDpsUserSelectable()).isEmpty()
    assertThat(types.filter { it.code == "NOT_DPS" }).isEmpty()
  }

  private fun List<CaseNoteType>.inactive() = flatMap { it.subCodes }.filter { it.activeFlag == ActiveYn.N }
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
