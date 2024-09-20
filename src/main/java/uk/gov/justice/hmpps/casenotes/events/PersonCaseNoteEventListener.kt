package uk.gov.justice.hmpps.casenotes.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import uk.gov.justice.hmpps.casenotes.config.ServiceConfig
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.publish

@Component
class PersonCaseNoteEventListener(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  private val serviceConfig: ServiceConfig,
  private val telemetryClient: TelemetryClient,
) {
  private val eventTopic by lazy { hmppsQueueService.findByTopicId("domainevents") as HmppsTopic }

  @TransactionalEventListener
  fun handlePersonCaseNoteEvent(event: PersonCaseNoteEvent) {
    if (serviceConfig.publishPersonEvents) {
      eventTopic.publish(
        event.eventName,
        objectMapper.writeValueAsString(event.asDomainEvent(serviceConfig.baseUrl)),
        event.attributes(),
      )
    } else {
      telemetryClient.trackEvent(event.eventName, event.properties(), mapOf())
    }
  }

  private fun PersonCaseNoteEvent.attributes() =
    mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventName).build())

  private fun PersonCaseNoteEvent.properties(): Map<String, String> = mapOf(
    "personIdentifier" to personIdentifier,
    "id" to id.toString(),
    "legacyId" to legacyId.toString(),
    "type" to type,
    "subType" to subType,
    "source" to source.toString(),
    "syncToNomis" to syncToNomis.toString(),
    "systemGenerated" to systemGenerated.toString(),
  )
}
