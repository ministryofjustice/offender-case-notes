package uk.gov.justice.hmpps.casenotes.events

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.hmpps.casenotes.alertnotes.AlertCaseNoteHandler
import uk.gov.justice.hmpps.casenotes.alertnotes.AlertCaseNoteReconciliation
import uk.gov.justice.hmpps.casenotes.merge.MergeEventHandler

@Service
class DomainEventListener(
  private val jsonMapper: JsonMapper,
  private val mergeEventHandler: MergeEventHandler,
  private val alertReconciliation: AlertCaseNoteReconciliation,
  private val alertCaseNotes: AlertCaseNoteHandler,
) {
  @SqsListener("domaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun receive(notification: Notification) {
    when (notification.eventType) {
      PRISONER_MERGED -> mergeEventHandler.handle(jsonMapper.readValue(notification.message))
      RECONCILE_ALERTS -> alertReconciliation.reconcile(jsonMapper.readValue(notification.message))
      ALERT_CREATED -> alertCaseNotes.handleAlertCreated(jsonMapper.readValue(notification.message))
      ALERT_INACTIVE -> alertCaseNotes.handleAlertInactive(jsonMapper.readValue(notification.message))
      else -> return
    }
  }

  companion object {
    const val PRISONER_MERGED = "prison-offender-events.prisoner.merged"
    const val RECONCILE_ALERTS = "case-notes.alerts.reconciliation"
    const val ALERT_CREATED = "person.alert.created"
    const val ALERT_INACTIVE = "person.alert.inactive"
  }
}
