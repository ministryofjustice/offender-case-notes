package uk.gov.justice.hmpps.casenotes.controllers

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.*

open class PublishNoteResourceTest : ResourceTest() {
  @MockBean
  private lateinit var repository: OffenderCaseNoteRepository

  @Test
  open fun testPublishCaseNotes_AccessDenied() {
    whenever(repository.findByModifyDateTimeBetweenOrderByModifyDateTime(any(), any())).thenReturn(
        listOf(createOffenderCaseNote(SensitiveCaseNoteType.builder().type("subtype").parentType(ParentNoteType.builder().type("parent").build()).build()))
    )
    val response: ResponseEntity<String>? = testRestTemplate.exchange(
        "/publish-notes?toDateTime={toDateTime}",
        HttpMethod.POST,
        createHttpEntityWithBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES),
        object : ParameterizedTypeReference<String>() {}, now())
    assertThatStatus<String>(response, 403)
  }

  @Test
  open fun testPublishCaseNotesNoFrom() {
    whenever(repository.findByModifyDateTimeBetweenOrderByModifyDateTime(any(), any())).thenReturn(
        listOf(createOffenderCaseNote(SensitiveCaseNoteType.builder().type("subtype").parentType(ParentNoteType.builder().type("parent").build()).build()))
    )
    val toDate: LocalDateTime = now()
    val response: ResponseEntity<String>? = testRestTemplate.exchange(
        "/publish-notes?toDateTime={toDateTime}",
        HttpMethod.POST,
        createHttpEntityWithBearerAuthorisation("SECURE_CASENOTE_USER", PUBLISH_ROLE),
        object : ParameterizedTypeReference<String>() {}, toDate)
    assertThatStatus<String>(response, 200)
    assertThat(response!!.body).isEqualTo("1")
    verify(repository).findByModifyDateTimeBetweenOrderByModifyDateTime(LocalDateTime.parse("2019-01-01T00:00:00"), toDate)
  }

  @Test
  open fun testPublishCaseNotes_FromAndTo() {
    whenever(repository.findByModifyDateTimeBetweenOrderByModifyDateTime(any(), any())).thenReturn(
        listOf(createOffenderCaseNote(SensitiveCaseNoteType.builder().type("subtype").parentType(ParentNoteType.builder().type("parent").build()).build()))
    )
    val toDate: LocalDateTime = now()
    val fromDate: LocalDateTime = LocalDateTime.parse("2019-01-02T02:03:04")
    val response: ResponseEntity<String>? = testRestTemplate.exchange(
        "/publish-notes?toDateTime={toDateTime}&fromDateTime={fromDateTime}",
        HttpMethod.POST,
        createHttpEntityWithBearerAuthorisation("SECURE_CASENOTE_USER", PUBLISH_ROLE),
        object : ParameterizedTypeReference<String>() {}, toDate, fromDate)
    assertThatStatus<String>(response, 200)
    assertThat(response!!.body).isEqualTo("1")
    verify(repository).findByModifyDateTimeBetweenOrderByModifyDateTime(fromDate, toDate)
  }

  private fun createOffenderCaseNote(caseNoteType: SensitiveCaseNoteType): OffenderCaseNote =
      OffenderCaseNote.builder()
          .id(UUID.randomUUID())
          .occurrenceDateTime(now())
          .locationId("MDI")
          .offenderIdentifier("A1234AC")
          .sensitiveCaseNoteType(caseNoteType)
          .noteText("HELLO")
          .createDateTime(LocalDateTime.parse("2019-02-03T23:20:19"))
          .build()


  companion object {
    private val CASENOTES_ROLES: List<String> = listOf("ROLE_VIEW_SENSITIVE_CASE_NOTES", "ROLE_ADD_SENSITIVE_CASE_NOTES")
    private val PUBLISH_ROLE: List<String> = listOf("ROLE_PUBLISH_SENSITIVE_CASE_NOTES")
  }
}
