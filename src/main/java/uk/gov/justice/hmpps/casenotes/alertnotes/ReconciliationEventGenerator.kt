package uk.gov.justice.hmpps.casenotes.alertnotes

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.BackOffPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.DomainEventListener
import uk.gov.justice.hmpps.casenotes.events.MessageAttributes
import uk.gov.justice.hmpps.casenotes.events.Notification
import uk.gov.justice.hmpps.casenotes.events.PersonReference
import uk.gov.justice.hmpps.sqs.DEFAULT_BACKOFF_POLICY
import uk.gov.justice.hmpps.sqs.DEFAULT_RETRY_POLICY
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Service
class ReconciliationEventGenerator(
  private val queueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  private val alertService: AlertService,
) {
  private val eventQueue: HmppsQueue by lazy {
    queueService.findByQueueId("domaineventsqueue") ?: throw IllegalStateException("Queue not available")
  }
  fun generateEventsFor(from: LocalDate, to: LocalDate) {
    alertService.getPrisonNumbersOfInterest(from, to).personIdentifiers
      .asSequence()
      .map { reconciliationEvent(it, from, to) }
      .chunked(10)
      .forEach { eventQueue.publishBatch(it) }
  }

  private fun reconciliationEvent(
    personIdentifier: String,
    from: LocalDate,
    to: LocalDate,
  ): DomainEvent<AlertReconciliationInformation> = DomainEvent(
    ZonedDateTime.now(),
    DomainEventListener.RECONCILE_ALERTS,
    null,
    "Reconcile Alert Case Notes",
    AlertReconciliationInformation(personIdentifier, from, to),
    PersonReference.withIdentifier(personIdentifier),
  )

  private fun HmppsQueue.publishBatch(
    events: Collection<DomainEvent<AlertReconciliationInformation>>,
    retryPolicy: RetryPolicy = DEFAULT_RETRY_POLICY,
    backOffPolicy: BackOffPolicy = DEFAULT_BACKOFF_POLICY,
  ) {
    val retryTemplate =
      RetryTemplate().apply {
        setRetryPolicy(retryPolicy)
        setBackOffPolicy(backOffPolicy)
      }
    val publishRequest =
      SendMessageBatchRequest
        .builder()
        .queueUrl(queueUrl)
        .entries(
          events.map {
            val notification =
              Notification(objectMapper.writeValueAsString(it), attributes = MessageAttributes(it.eventType))
            SendMessageBatchRequestEntry
              .builder()
              .id(UUID.randomUUID().toString())
              .messageBody(objectMapper.writeValueAsString(notification))
              .build()
          },
        ).build()
    retryTemplate.execute<SendMessageBatchResponse, RuntimeException> {
      sqsClient.sendMessageBatch(publishRequest).get()
    }
  }
}
