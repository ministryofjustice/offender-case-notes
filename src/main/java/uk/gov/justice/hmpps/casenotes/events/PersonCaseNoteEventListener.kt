package uk.gov.justice.hmpps.casenotes.events

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.hmpps.casenotes.config.ServiceConfig
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.publish

@Component
class PersonCaseNoteEventListener(
  private val hmppsQueueService: HmppsQueueService,
  private val jsonMapper: JsonMapper,
  private val serviceConfig: ServiceConfig,
) {
  private val eventTopic by lazy { hmppsQueueService.findByTopicId("domainevents") as HmppsTopic }

  @TransactionalEventListener
  fun handlePersonCaseNoteEvent(event: PersonCaseNoteEvent) {
    eventTopic.publish(
      event.eventName,
      jsonMapper.writeValueAsString(event.asDomainEvent(serviceConfig.baseUrl)),
      attributes = mapOf(
        "type" to MessageAttributeValue.builder().dataType("String").stringValue(event.type).build(),
        "subType" to MessageAttributeValue.builder().dataType("String").stringValue(event.subType).build(),
      ),
    )
  }
}
