package uk.gov.justice.hmpps.casenotes.services

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EventListener(
  private val caseNoteService: CaseNoteService,
  private val mergeOffenderService: MergeOffenderService,
  private val objectMapper: ObjectMapper,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("event", factory = "hmppsQueueContainerFactoryProxy")
  fun handleEvents(requestJson: String) {
    val (message, messageAttributes) = objectMapper.readValue<Message>(requestJson)
    val (offenderIdDisplay, bookingId) = objectMapper.readValue<EventMessage>(message)

    val eventType = messageAttributes.eventType.value
    log.info("Processing message of type {}", eventType)

    when (eventType) {
      "BOOKING_NUMBER-CHANGED" -> mergeOffenderService.checkAndMerge(bookingId)
      "DATA_COMPLIANCE_DELETE-OFFENDER" -> caseNoteService.deleteCaseNotesForOffender(offenderIdDisplay)
    }
  }
}

data class Attribute(@JsonProperty("Type") val type: String, @JsonProperty("Value") val value: String)
data class MessageAttributes(val eventType: Attribute)
data class EventMessage(val offenderIdDisplay: String?, val bookingId: Long)
data class Message(
  @JsonProperty("Message") val message: String,
  @JsonProperty("MessageAttributes") val messageAttributes: MessageAttributes,
)
