package uk.gov.justice.hmpps.casenotes.health

import org.awaitility.kotlin.await
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

abstract class QueueListenerIntegrationTest : IntegrationTest() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  internal val eventQueue by lazy { hmppsQueueService.findByQueueId("event") as HmppsQueue }

  internal val eventQueueSqsClient by lazy { eventQueue.sqsClient }
  internal val eventQueueName by lazy { eventQueue.queueName }
  internal val eventQueueUrl by lazy { eventQueue.queueUrl }

  internal val eventSqsDlqClient by lazy { eventQueue.sqsDlqClient as SqsAsyncClient }
  internal val eventDlqName by lazy { eventQueue.dlqName as String }
  internal val eventDlqUrl by lazy { eventQueue.dlqUrl as String }

  fun purgeQueues() {
    eventQueueSqsClient.purgeQueue(software.amazon.awssdk.services.sqs.model.PurgeQueueRequest.builder().queueUrl(eventQueueUrl).build())
    await.until { getNumberOfMessagesCurrentlyOnPrisonEventQueue() == 0 }
    eventSqsDlqClient.purgeQueue(software.amazon.awssdk.services.sqs.model.PurgeQueueRequest.builder().queueUrl(eventDlqUrl).build())
    await.until { getNumberOfMessagesCurrentlyOnPrisonEventDlq() == 0 }
  }

  fun getNumberOfMessagesCurrentlyOnPrisonEventQueue(): Int = eventQueueSqsClient.countMessagesOnQueue(eventQueueUrl).get()
  fun getNumberOfMessagesCurrentlyOnPrisonEventDlq(): Int = eventSqsDlqClient.countMessagesOnQueue(eventDlqUrl).get()
}
