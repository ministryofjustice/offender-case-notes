package uk.gov.justice.hmpps.casenotes.e2e

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.hmpps.casenotes.health.QueueListenerIntegrationTest

class HouseKeepingIntegrationTest : QueueListenerIntegrationTest() {
  @BeforeEach
  internal fun setUp() {
    purgeQueues()
  }

  @Test
  fun `housekeeping will consume a booking changed message on the dlq and return to main queue`() {
    val message = "/messages/bookingNumberChanged.json".readResourceAsText()
    eventQueueSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(eventDlqUrl)
        .messageBody(message)
        .build(),
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonEventDlq() } matches { it == 1 }

    webTestClient.put()
      .uri("/queue-admin/retry-all-dlqs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk

    await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonEventDlq() } matches { it == 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonEventQueue() } matches { it == 0 }
  }
}

private fun String.readResourceAsText(): String {
  return HouseKeepingIntegrationTest::class.java.getResource(this).readText()
}
