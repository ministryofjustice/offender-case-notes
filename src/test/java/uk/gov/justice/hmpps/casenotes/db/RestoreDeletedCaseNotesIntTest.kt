package uk.gov.justice.hmpps.casenotes.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.hmpps.casenotes.config.CaseloadIdHeader
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.controllers.ACTIVE_PRISON
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import uk.gov.justice.hmpps.casenotes.controllers.USERNAME
import uk.gov.justice.hmpps.casenotes.notes.CaseNote

class RestoreDeletedCaseNotesIntTest : IntegrationTest() {
  @Test
  @Sql("classpath:jpa/repository/reset.sql")
  @Sql("classpath:resources/casenotes/deleted_case_notes.sql")
  @Sql("classpath:resources/casenotes/restore_deleted_case_notes.sql")
  fun `can read a restored case note by id`() {
    val response = getCaseNote(
      "A0001AA",
      "0192007e-abff-7acb-9282-341ffff52e29",
      listOf(SecurityUserContext.ROLE_CASE_NOTES_READ),
    ).success<CaseNote>()

    assertThat(response.id).isEqualTo("0192007e-abff-7acb-9282-341ffff52e29")
  }

  private fun getCaseNote(
    personIdentifier: String,
    caseNoteId: String,
    roles: List<String> = listOf(SecurityUserContext.ROLE_CASE_NOTES_READ),
    username: String = USERNAME,
    caseloadId: String? = ACTIVE_PRISON,
  ) = webTestClient.get().uri(urlToTest(personIdentifier, caseNoteId)).headers(addBearerAuthorisation(username, roles))
    .apply { if (caseloadId != null) this.header(CaseloadIdHeader.NAME, caseloadId) }.exchange()

  private fun urlToTest(personIdentifier: String, caseNoteId: String) = "/case-notes/$personIdentifier/$caseNoteId"
}
