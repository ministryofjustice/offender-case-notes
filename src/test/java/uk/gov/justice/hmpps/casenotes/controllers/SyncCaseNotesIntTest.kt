package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_SYNC
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.sync.SyncAmendmentRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncResult
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.prisonNumber
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

class SyncCaseNotesIntTest : ResourceTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(SYNC_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    syncCaseNotes(listOf(syncCaseNoteRequest()), listOf(ROLE_CASE_NOTES_WRITE)).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - case note types must exist`() {
    val request = listOf(
      syncCaseNoteRequest(type = "NON_EXISTENT", subType = "GARBAGE"),
      syncCaseNoteRequest(type = "NON_EXISTENT", subType = "NOT_THERE"),
      syncCaseNoteRequest(type = "ANOTHER_TYPE", subType = "GARBAGE"),
    )
    val response = syncCaseNotes(request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Case note types missing: { ANOTHER_TYPE:[GARBAGE], NON_EXISTENT:[GARBAGE, NOT_THERE] }")
    }
  }

  @Test
  fun `400 bad request - case note types be sync to nomis types`() {
    val request = listOf(
      syncCaseNoteRequest(type = "POM", subType = "SPECIAL"),
      syncCaseNoteRequest(type = "POM", subType = "GEN"),
      syncCaseNoteRequest(type = "OMIC", subType = "GEN"),
      syncCaseNoteRequest(type = "OMIC", subType = "OMIC_OPEN"),
    )
    val response = syncCaseNotes(request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Case note types are not sync to nomis types: { OMIC:[GEN, OMIC_OPEN], POM:[GEN, SPECIAL] }")
    }
  }

  @Test
  fun `200 ok - new case notes created`() {
    val prisonNumbers = (0..100).map { prisonNumber() }
    val types = getAllTypes().filter { it.syncToNomis }
    val request = (0..5_000).map {
      val type = types.random()
      syncCaseNoteRequest(
        prisonIdentifier = prisonNumbers.random(),
        type = type.parent.code,
        subType = type.code,
        amendments = if (it % 5 == 0) setOf(syncAmendmentRequest(), syncAmendmentRequest()) else setOf(),
      )
    }
    val (response, elapsed) = measureTimedValue { syncCaseNotes(request).successList<SyncResult>() }
    assertThat(response.size).isEqualTo(request.size)
    assertThat(elapsed.toJavaDuration()).isLessThanOrEqualTo(Duration.ofSeconds(2))
  }

  private fun syncCaseNotes(
    request: List<SyncCaseNoteRequest>,
    roles: List<String> = listOf(ROLE_CASE_NOTES_SYNC),
    username: String = "NOMIS_TO_DPS",
  ) = webTestClient.put().uri(SYNC_URL)
    .headers(addBearerAuthorisation(username, roles))
    .bodyValue(request)
    .exchange()

  companion object {
    private const val SYNC_URL = "/sync/case-notes"
  }
}

private fun syncCaseNoteRequest(
  legacyId: Long = NomisIdGenerator.newId(),
  id: UUID? = null,
  prisonIdentifier: String = prisonNumber(),
  locationId: String = "SWI",
  type: String = "OMIC",
  subType: String = "GEN",
  occurrenceDateTime: LocalDateTime = LocalDateTime.now().minusDays(2),
  text: String = "The text of the case note",
  systemGenerated: Boolean = false,
  authorUsername: String = "AuthorUsername",
  authorUserId: String = "12376471",
  authorName: String = "Author Name",
  createdDateTime: LocalDateTime = LocalDateTime.now().minusDays(1),
  source: Source = Source.NOMIS,
  amendments: Set<SyncAmendmentRequest> = setOf(),
) = SyncCaseNoteRequest(
  legacyId,
  id,
  prisonIdentifier,
  locationId,
  type,
  subType,
  occurrenceDateTime,
  text,
  systemGenerated,
  authorUsername,
  authorUserId,
  authorName,
  createdDateTime,
  source,
  amendments,
)

private fun syncAmendmentRequest(
  text: String = "The text of the case note",
  authorUsername: String = "AuthorUsername",
  authorUserId: String = "12376471",
  authorName: String = "Author Name",
  createdDateTime: LocalDateTime = LocalDateTime.now(),
) = SyncAmendmentRequest(text, authorUsername, authorUserId, authorName, createdDateTime)
