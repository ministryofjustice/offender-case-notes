package uk.gov.justice.hmpps.casenotes.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@RunWith(MockitoJUnitRunner::class)
class CaseNoteAwsEventPusherTest {
  private val snsClient: SnsAsyncClient = mock()
  private val objectMapper: ObjectMapper = mock()

  private lateinit var service: CaseNoteAwsEventPusher

  @Before
  fun before() {
    service = CaseNoteAwsEventPusher(snsClient, "topicArn", objectMapper)
  }

  @Test
  fun `send event converts to case note event`() {
    whenever(objectMapper.writeValueAsString(any())).thenReturn("messageAsJson")
    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId("Hello").build()))
    service.sendEvent(caseCaseNote())
    verify(objectMapper).writeValueAsString(check<CaseNoteEvent> {
      assertThat(it).isEqualTo(CaseNoteEvent(
          eventType = "GEN-OSE",
          eventDatetime = LocalDateTime.parse("2019-03-04T10:11:12"),
          offenderIdDisplay = "A1234AC",
          agencyLocationId = "MDI",
          caseNoteId = "abcde"))
    })
  }

  @Test
  fun `send event sends to the sns client`() {
    whenever(objectMapper.writeValueAsString(any())).thenReturn("messageAsJson")
    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId("Hello").build()))
    service.sendEvent(caseCaseNote())
    verify(snsClient).publish(check<PublishRequest> {
      assertThat(it.message()).isEqualTo("messageAsJson")
      assertThat(it.topicArn()).isEqualTo("topicArn")
      assertThat(it.messageAttributes()).containsEntry("eventType", MessageAttributeValue.builder().dataType("String").stringValue("GEN-OSE").build())
    })
  }

  private fun caseCaseNote(): CaseNote {
    return CaseNote.builder()
        .caseNoteId("abcde")
        .creationDateTime(LocalDateTime.parse("2019-03-04T10:11:12"))
        .occurrenceDateTime(LocalDateTime.parse("2018-02-03T10:11:12"))
        .locationId("MDI")
        .authorUserId("some user")
        .authorName("Mickey Mouse")
        .offenderIdentifier("A1234AC")
        .type("GEN")
        .subType("OSE")
        .text("HELLO")
        .build()
  }
}
