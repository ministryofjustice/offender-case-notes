package uk.gov.justice.hmpps.casenotes.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.hmpps.casenotes.legacy.service.CaseNoteAdditionalInformation
import uk.gov.justice.hmpps.casenotes.legacy.service.CaseNoteAwsEventPusher
import uk.gov.justice.hmpps.casenotes.legacy.service.HmppsDomainEvent
import uk.gov.justice.hmpps.casenotes.legacy.service.PersonIdentifier
import uk.gov.justice.hmpps.casenotes.legacy.service.PersonReference
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class CaseNoteAwsEventPusherTest {
  private val hmppsQueueService: HmppsQueueService = mock()
  private val snsClient: SnsAsyncClient = mock()
  private val objectMapper: ObjectMapper = mock()
  private val service = CaseNoteAwsEventPusher(hmppsQueueService, objectMapper, "http://localhost:8080")

  @Test
  fun `send event converts to case note event`() {
    whenever(objectMapper.writeValueAsString(any())).thenReturn("messageAsJson")
    whenever(hmppsQueueService.findByTopicId("domainevents")).thenReturn(HmppsTopic("id", "topicUrn", snsClient))

    val publishResponse = PublishResponse.builder().messageId("Hello").build()
    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(completedFuture(publishResponse))

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
  fun `send event sends to the sns client1`() {
    whenever(objectMapper.writeValueAsString(any())).thenReturn("messageAsJson")
    whenever(hmppsQueueService.findByTopicId("domainevents")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    val publishResponse = mock<PublishResponse>()
    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(completedFuture(publishResponse))

    service.sendEvent(caseCaseNote())
    // val  publishRequest =
    verify(snsClient).publish(
      PublishRequest.builder().message("messageAsJson")
        .topicArn("topicArn")
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue("prison.case-note.published")
              .build(),
            "contentType" to MessageAttributeValue.builder().dataType("String").stringValue("text/plain;charset=UTF-8")
              .build(),
            "caseNoteType" to MessageAttributeValue.builder().dataType("String").stringValue("GEN-OSE").build(),
          ),
        )
        .build(),

    )
  }

  private fun caseCaseNote(): CaseNote {
    return CaseNote.builder()
      .id("abcde")
      .createdAt(LocalDateTime.parse("2019-03-04T10:11:12"))
      .occurredAt(LocalDateTime.parse("2018-02-03T10:11:12"))
      .locationId("MDI")
      .authorUserId("some user")
      .authorUsername("SOME_USER")
      .authorName("Mickey Mouse")
      .personIdentifier("A1234AC")
      .type("GEN")
      .typeDescription("Description of GEN")
      .subType("OSE")
      .subTypeDescription("Description of OSE")
      .text("HELLO")
      .source("OCNS")
      .build()
  }
}
