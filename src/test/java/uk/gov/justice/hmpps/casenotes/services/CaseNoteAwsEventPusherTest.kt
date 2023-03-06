package uk.gov.justice.hmpps.casenotes.services

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishResult
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.LocalDateTime

class CaseNoteAwsEventPusherTest {
  private val hmppsQueueService: HmppsQueueService = mock()
  private val snsClient: AmazonSNS = mock()
  private val objectMapper: ObjectMapper = mock()

  private val service = CaseNoteAwsEventPusher(hmppsQueueService, objectMapper, "http://localhost:8080")

  @Test
  fun `send event converts to case note event`() {
    whenever(objectMapper.writeValueAsString(any())).thenReturn("messageAsJson")
    whenever(hmppsQueueService.findByTopicId("domainevents")).thenReturn(HmppsTopic("id", "topicUrn", snsClient))
    whenever(snsClient.publish(any())).thenReturn(PublishResult().withMessageId("Hello"))
    service.sendEvent(caseCaseNote())
    verify(objectMapper).writeValueAsString(
      check<HmppsDomainEvent> {
        assertThat(it).isEqualTo(
          HmppsDomainEvent(
            eventType = "prison.case-note.published",
            detailUrl = "http://localhost:8080/case-notes/A1234AC/abcde",
            occurredAt = LocalDateTime.parse("2019-03-04T10:11:12"),
            personReference = PersonReference(identifiers = listOf(PersonIdentifier("NOMS", "A1234AC"))),
            additionalInformation = CaseNoteAdditionalInformation(
              caseNoteId = "abcde",
              caseNoteType = "GEN-OSE",
            ),
          ),
        )
      },
    )
  }

  @Test
  fun `send event sends to the sns client`() {
    whenever(objectMapper.writeValueAsString(any())).thenReturn("messageAsJson")
    whenever(hmppsQueueService.findByTopicId("domainevents")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any())).thenReturn(PublishResult().withMessageId("Hello"))
    service.sendEvent(caseCaseNote())
    verify(snsClient).publish(
      check {
        assertThat(it.message).isEqualTo("messageAsJson")
        assertThat(it.topicArn).isEqualTo("topicArn")
        assertThat(it.messageAttributes).containsEntry(
          "eventType",
          MessageAttributeValue().withDataType("String").withStringValue("prison.case-note.published"),
        )
        assertThat(it.messageAttributes).containsEntry(
          "caseNoteType",
          MessageAttributeValue().withDataType("String").withStringValue("GEN-OSE"),
        )
      },
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
