package uk.gov.justice.hmpps.casenotes.health

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import org.awaitility.kotlin.await
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

abstract class QueueListenerIntegrationTest : IntegrationTest() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  internal val eventQueue by lazy { hmppsQueueService.findByQueueId("event") as HmppsQueue }

  internal val eventQueueSqsClient by lazy { eventQueue.sqsClient }
  internal val eventQueueName by lazy { eventQueue.queueName }
  internal val eventQueueUrl by lazy { eventQueue.queueUrl }

  internal val eventSqsDlqClient by lazy { eventQueue.sqsDlqClient as AmazonSQS }
  internal val eventDlqName by lazy { eventQueue.dlqName as String }
  internal val eventDlqUrl by lazy { eventQueue.dlqUrl as String }

  fun purgeQueues() {
    eventQueueSqsClient.purgeQueue(PurgeQueueRequest(eventQueueUrl))
    await.until { getNumberOfMessagesCurrentlyOnPrisonEventQueue() == 0 }
    eventSqsDlqClient.purgeQueue(PurgeQueueRequest(eventDlqUrl))
    await.until { getNumberOfMessagesCurrentlyOnPrisonEventDlq() == 0 }
  }

  fun getNumberOfMessagesCurrentlyOnPrisonEventQueue(): Int = eventQueueSqsClient.numMessages(eventQueueUrl)
  fun getNumberOfMessagesCurrentlyOnPrisonEventDlq(): Int = eventSqsDlqClient.numMessages(eventDlqUrl)
}

fun AmazonSQS.numMessages(url: String): Int {
  val queueAttributes = getQueueAttributes(url, listOf("ApproximateNumberOfMessages"))
  return queueAttributes.attributes["ApproximateNumberOfMessages"]!!.toInt()
}
