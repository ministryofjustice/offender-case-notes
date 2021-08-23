package uk.gov.justice.hmpps.casenotes.services

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.model.PublishResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.LocalDateTime

class CaseNoteAwsEventPusherTest {
  private val hmppsQueueService: HmppsQueueService = mock()
  private val snsClient: AmazonSNS = mock()
  private val objectMapper: ObjectMapper = mock()

  private val service = CaseNoteAwsEventPusher(hmppsQueueService, objectMapper)

  @Test
  fun `send event converts to case note event`() {
    whenever(objectMapper.writeValueAsString(any())).thenReturn("messageAsJson")
    whenever( hmppsQueueService.findByTopicId("offenderevents")).thenReturn(HmppsTopic("id", "topicUrn", snsClient))
    whenever(snsClient.publish(any())).thenReturn(PublishResult().withMessageId("Hello"))
    service.sendEvent(caseCaseNote())
    verify(objectMapper).writeValueAsString(
      check<CaseNoteEvent> {
        assertThat(it).isEqualTo(
          CaseNoteEvent(
            eventType = "GEN-OSE",
            eventDatetime = LocalDateTime.parse("2019-03-04T10:11:12"),
            offenderIdDisplay = "A1234AC",
            agencyLocationId = "MDI",
            caseNoteId = "abcde"
          )
        )
      }
    )
  }

  @Test
  fun `send event sends to the sns client`() {
    whenever(objectMapper.writeValueAsString(any())).thenReturn("messageAsJson")
    whenever( hmppsQueueService.findByTopicId("offenderevents")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any())).thenReturn(PublishResult().withMessageId("Hello"))
    service.sendEvent(caseCaseNote())
    verify(snsClient).publish(
      check<PublishRequest> {
        assertThat(it.message).isEqualTo("messageAsJson")
        assertThat(it.topicArn).isEqualTo("topicArn")
        assertThat(it.messageAttributes).containsEntry("eventType", MessageAttributeValue().withDataType("String").withStringValue("GEN-OSE"))
      }
    )
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
