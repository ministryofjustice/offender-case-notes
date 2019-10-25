package uk.gov.justice.hmpps.casenotes.services

import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import java.time.LocalDateTime

interface CaseNoteEventPusher {
  fun sendEvent(caseNote: CaseNote)

  fun isSensitiveCaseNote(caseNoteIdentifier: String): Boolean {
    return !NumberUtils.isDigits(caseNoteIdentifier)
  }
}

@Component
@ConditionalOnProperty(name = ["sqs.provider"])
open class CaseNoteAwsEventPusher(private val jmsTemplate: JmsTemplate) : CaseNoteEventPusher {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun sendEvent(caseNote: CaseNote) {
    if (isSensitiveCaseNote(caseNote.caseNoteId)) {
      log.debug("Pushing case note {} to event topic", caseNote.caseNoteId)
      jmsTemplate.convertAndSend(CaseNoteEvent(caseNote))
    }
  }
}

@Component
@ConditionalOnProperty("sqs.provider", matchIfMissing = true, havingValue = "no value set")
open class CaseNoteNoOpEventPusher() : CaseNoteEventPusher {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun sendEvent(caseNote: CaseNote) {
    if (isSensitiveCaseNote(caseNote.caseNoteId)) {
      log.warn("Pretending to push case note {} to event topic", caseNote.caseNoteId)
      log.debug("Case note not sent was {}", CaseNoteEvent(caseNote))
    }
  }
}

data class CaseNoteEvent(val eventType: String, val eventDatetime: LocalDateTime,
                         val offenderIdDisplay: String, val agencyLocationId: String, val caseNoteId: String) {
  constructor(cn: CaseNote) : this(
      eventType = "${cn.type}-${cn.subType}",
      eventDatetime = cn.creationDateTime,
      offenderIdDisplay = cn.offenderIdentifier,
      agencyLocationId = cn.locationId,
      caseNoteId = cn.caseNoteId)
}
