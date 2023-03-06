package uk.gov.justice.hmpps.casenotes.controllers

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.hmpps.casenotes.model.CaseNoteType
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

class PublishNoteResourceTest : ResourceTest() {
  @MockBean
  private lateinit var repository: OffenderCaseNoteRepository

  @Test
  fun testPublishCaseNotes_AccessDenied() {
    whenever(repository.findByModifyDateTimeBetweenOrderByModifyDateTime(any(), any())).thenReturn(
      listOf(
        createOffenderCaseNote(
          CaseNoteType.builder().type("subtype").parentType(ParentNoteType.builder().type("parent").build()).build(),
        ),
      ),
    )
    webTestClient.post().uri("/publish-notes?toDateTime={toDateTime}", now())
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun testPublishCaseNotesNoFrom() {
    whenever(repository.findByModifyDateTimeBetweenOrderByModifyDateTime(any(), any())).thenReturn(
      listOf(
        createOffenderCaseNote(
          CaseNoteType.builder().type("subtype").parentType(ParentNoteType.builder().type("parent").build()).build(),
        ),
      ),
    )
    val toDate: LocalDateTime = now()
    webTestClient.post().uri("/publish-notes?toDateTime={toDateTime}", toDate)
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", PUBLISH_ROLE))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json("1")
    verify(repository).findByModifyDateTimeBetweenOrderByModifyDateTime(
      LocalDateTime.parse("2019-01-01T00:00:00"),
      toDate,
    )
  }

  @Test
  fun testPublishCaseNotes_FromAndTo() {
    whenever(repository.findByModifyDateTimeBetweenOrderByModifyDateTime(any(), any())).thenReturn(
      listOf(
        createOffenderCaseNote(
          CaseNoteType.builder().type("subtype").parentType(ParentNoteType.builder().type("parent").build()).build(),
        ),
      ),
    )
    val toDate: LocalDateTime = now()
    val fromDate: LocalDateTime = LocalDateTime.parse("2019-01-02T02:03:04")
    webTestClient.post().uri("/publish-notes?toDateTime={toDateTime}&fromDateTime={fromDateTime}", toDate, fromDate)
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", PUBLISH_ROLE))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json("1")
    verify(repository).findByModifyDateTimeBetweenOrderByModifyDateTime(fromDate, toDate)
  }

  private fun createOffenderCaseNote(caseNoteType: CaseNoteType): OffenderCaseNote =
    OffenderCaseNote.builder()
      .id(UUID.randomUUID())
      .occurrenceDateTime(now())
      .locationId("MDI")
      .offenderIdentifier("A1234AC")
      .caseNoteType(caseNoteType)
      .noteText("HELLO")
      .createDateTime(LocalDateTime.parse("2019-02-03T23:20:19"))
      .build()

  companion object {
    private val CASENOTES_ROLES = listOf("ROLE_VIEW_SENSITIVE_CASE_NOTES", "ROLE_ADD_SENSITIVE_CASE_NOTES")
    private val PUBLISH_ROLE = listOf("ROLE_PUBLISH_SENSITIVE_CASE_NOTES")
  }
}
