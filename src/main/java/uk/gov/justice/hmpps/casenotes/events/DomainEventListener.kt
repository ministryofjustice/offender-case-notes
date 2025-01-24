package uk.gov.justice.hmpps.casenotes.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.casenotes.alertbackfill.AlertCaseNoteVerification
import uk.gov.justice.hmpps.casenotes.merge.MergeEventHandler

@Service
class DomainEventListener(
  private val objectMapper: ObjectMapper,
  private val mergeEventHandler: MergeEventHandler,
  private val alertCaseNotes: AlertCaseNoteVerification,
) {
  @SqsListener("domaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun receive(notification: Notification) {
    when (notification.eventType) {
      PRISONER_MERGED -> mergeEventHandler.handle(objectMapper.readValue(notification.message))
      VERIFY_ALERTS -> alertCaseNotes.verify(objectMapper.readValue(notification.message))
      else -> return
    }
  }

  companion object {
    const val PRISONER_MERGED = "prison-offender-events.prisoner.merged"
    const val VERIFY_ALERTS = "case-notes.alerts.verification"
  }
}
