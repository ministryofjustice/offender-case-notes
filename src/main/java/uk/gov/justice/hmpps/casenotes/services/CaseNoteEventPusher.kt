package uk.gov.justice.hmpps.casenotes.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import java.time.LocalDateTime

interface CaseNoteEventPusher {
  fun sendEvent(caseNote: CaseNote)

  fun isSensitiveCaseNote(caseNoteIdentifier: String): Boolean {
    return !NumberUtils.isDigits(caseNoteIdentifier)
  }
}

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
@ConditionalOnProperty(name = ["sns.provider"])
open class CaseNoteAwsEventPusher(private val snsClient: SnsAsyncClient,
                                  @Value("\${sns.topic.arn}") private val topicArn: String,
                                  private val objectMapper: ObjectMapper) : CaseNoteEventPusher {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun sendEvent(caseNote: CaseNote) {
    if (isSensitiveCaseNote(caseNote.caseNoteId)) {
      val cne = CaseNoteEvent(caseNote)
      log.info("Pushing case note {} to event topic with event type of {}", cne.caseNoteId, cne.eventType)
      val publishRequest = PublishRequest.builder()
          .topicArn(topicArn)
          .messageAttributes(mapOf(
              "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(cne.eventType).build(),
              "contentType" to MessageAttributeValue.builder().dataType("String").stringValue("text/plain;charset=UTF-8").build()
          ))
          .message(objectMapper.writeValueAsString(cne))
          .build()
      snsClient.publish(publishRequest)
          .whenComplete { publishResponse, throwable ->
            publishResponse?.run { log.debug("Sent case note with message id {}", publishResponse.messageId()) }
            throwable?.run { log.error("Failed to send case note", throwable) }
          }
    }
  }
}

@Component
@ConditionalOnProperty("sns.provider", matchIfMissing = true, havingValue = "no value set")
open class CaseNoteNoOpEventPusher : CaseNoteEventPusher {
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
