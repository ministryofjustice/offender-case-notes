package uk.gov.justice.hmpps.casenotes.services

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service

@Service
class EventListener(
  private val caseNoteService: CaseNoteService,
  private val mergeOffenderService: MergeOffenderService,
  private val gson: Gson
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @JmsListener(destination = "event", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun handleEvents(requestJson: String?) {
    val (Message, MessageAttributes) = gson.fromJson(requestJson, Message::class.java)
    val (offenderIdDisplay, bookingId) = gson.fromJson(Message, EventMessage::class.java)

    val eventType = MessageAttributes.eventType.Value
    log.info("Processing message of type {}", eventType)

    when (eventType) {
      "BOOKING_NUMBER-CHANGED" -> mergeOffenderService.checkAndMerge(bookingId)
      "DATA_COMPLIANCE_DELETE-OFFENDER" -> caseNoteService.deleteCaseNotesForOffender(offenderIdDisplay)
    }
  }
}

data class Attribute(val Type: String, val Value: String)
data class MessageAttributes(val eventType: Attribute)
data class EventMessage(val offenderIdDisplay: String, val bookingId: Long)
data class Message(val Message: String, val MessageAttributes: MessageAttributes)
