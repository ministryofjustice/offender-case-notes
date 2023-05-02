package uk.gov.justice.hmpps.casenotes.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.net.URI
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
  private val objectMapper: ObjectMapper,
  @Value("\${casenotes.api.base.url}") private val caseNotesApiBaseUrl: String,
) : CaseNoteEventPusher {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  internal val eventTopic by lazy { hmppsQueueService.findByTopicId("domainevents") as HmppsTopic }
  internal val snsClient by lazy { eventTopic.snsClient }
  internal val topicArn by lazy { eventTopic.arn }

  override fun sendEvent(caseNote: CaseNote) {
    if (isSensitiveCaseNote(caseNote.caseNoteId)) {
      val cne = HmppsDomainEvent(caseNote, caseNotesApiBaseUrl)
      log.info("Pushing case note {} to event topic with event type of {}", caseNote.caseNoteId, cne.eventType)
      try {
        val publishResponse = snsClient.publish(
          PublishRequest.builder()
            .topicArn(topicArn)
            .message(objectMapper.writeValueAsString(cne))
            .messageAttributes(
              mapOf(
                "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(cne.eventType).build(),
                "contentType" to MessageAttributeValue.builder().dataType("String").stringValue("text/plain;charset=UTF-8").build(),
                "caseNoteType" to MessageAttributeValue.builder().dataType("String").stringValue(cne.additionalInformation.caseNoteType).build(),
              ),
            )
            .build(),
        )
        log.debug("Sent case note with message id {}", publishResponse.get().messageId())
      } catch (throwable: Throwable) {
        log.error("Failed to send case note", throwable)
      }
    }
  }
}

data class PersonIdentifier(val type: String, val value: String)
data class PersonReference(val identifiers: List<PersonIdentifier>)
data class CaseNoteAdditionalInformation(
  val caseNoteId: String,
  val caseNoteType: String,
)

data class HmppsDomainEvent(
  val version: Int = 1,
  val eventType: String = "prison.case-note.published",
  val description: String = "A prison case note has been created or amended",
  val detailUrl: String,
  val occurredAt: LocalDateTime,
  val personReference: PersonReference,
  val additionalInformation: CaseNoteAdditionalInformation,
) {
  constructor(caseNote: CaseNote, baseUrl: String) : this(
    detailUrl = URI.create("$baseUrl/case-notes/${caseNote.offenderIdentifier}/${caseNote.caseNoteId}").toString(),
    occurredAt = caseNote.creationDateTime,
    personReference = PersonReference(identifiers = listOf(PersonIdentifier("NOMS", caseNote.offenderIdentifier))),
    additionalInformation = CaseNoteAdditionalInformation(
      caseNoteId = caseNote.caseNoteId,
      caseNoteType = "${caseNote.type}-${caseNote.subType}",
    ),
  )
}
