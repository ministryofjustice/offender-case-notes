package uk.gov.justice.hmpps.casenotes.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service


@Service
open class EventListener(private val caseNoteService: CaseNoteService,
                         private val mergeOffenderService: MergeOffenderService) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    val gson: Gson = GsonBuilder().create()
  }

  @JmsListener(destination = "\${sqs.queue.name}")
  open fun handleEvents(requestJson: String?) {
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

