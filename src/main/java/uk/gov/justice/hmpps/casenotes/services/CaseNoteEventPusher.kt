package uk.gov.justice.hmpps.casenotes.services

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.LocalDateTime

interface CaseNoteEventPusher {
  fun sendEvent(caseNote: CaseNote)

  fun isSensitiveCaseNote(caseNoteIdentifier: String): Boolean {
    return !NumberUtils.isDigits(caseNoteIdentifier)
  }
}

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class CaseNoteAwsEventPusher(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper
) : CaseNoteEventPusher {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  internal val eventTopic by lazy { hmppsQueueService.findByTopicId("offenderevents") as HmppsTopic }
  internal val snsClient by lazy { eventTopic.snsClient }
  internal val topicArn by lazy { eventTopic.arn }

  override fun sendEvent(caseNote: CaseNote) {
    if (isSensitiveCaseNote(caseNote.caseNoteId)) {
      val cne = CaseNoteEvent(caseNote)
      log.info("Pushing case note {} to event topic with event type of {}", cne.caseNoteId, cne.eventType)
      val publishRequest = PublishRequest(
        topicArn,
        objectMapper.writeValueAsString(cne)
      ).withMessageAttributes(
        mapOf(
          "eventType" to MessageAttributeValue().withDataType("String").withStringValue(cne.eventType),
          "contentType" to MessageAttributeValue().withDataType("String").withStringValue("text/plain;charset=UTF-8")
        )
      )

      try {
        val publishResponse = snsClient.publish(publishRequest)
        log.debug("Sent case note with message id {}", publishResponse.messageId)
      } catch (throwable: Throwable) {
        log.error("Failed to send case note", throwable)
      }
    }
  }
}

data class CaseNoteEvent(
  val eventType: String,
  val eventDatetime: LocalDateTime,
  val offenderIdDisplay: String,
  val agencyLocationId: String,
  val caseNoteId: String
) {
  constructor(cn: CaseNote) : this(
    eventType = "${cn.type}-${cn.subType}",
    eventDatetime = cn.creationDateTime,
    offenderIdDisplay = cn.offenderIdentifier,
    agencyLocationId = cn.locationId,
    caseNoteId = cn.caseNoteId
  )
}
