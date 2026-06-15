package uk.gov.justice.hmpps.casenotes.events.pusher

import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.hmpps.casenotes.config.EuropeLondon
import uk.gov.justice.hmpps.casenotes.events.AdditionalInformation
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.PersonReference
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.publish
import java.net.URI

interface CaseNoteEventPusher {
  fun sendEvent(caseNote: CaseNote)

  fun isSensitiveCaseNote(caseNoteIdentifier: String): Boolean = !NumberUtils.isDigits(caseNoteIdentifier)
}

@Component
class CaseNoteAwsEventPusher(
  private val hmppsQueueService: HmppsQueueService,
  private val jsonMapper: JsonMapper,
  @param:Value($$"${service.base-url}") private val serviceBaseUrl: String,
) : CaseNoteEventPusher {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  internal val eventTopic by lazy { hmppsQueueService.findByTopicId("domainevents") as HmppsTopic }

  override fun sendEvent(caseNote: CaseNote) {
    if (isSensitiveCaseNote(caseNote.id)) {
      val cne = buildDomainEvent(caseNote, serviceBaseUrl)
      log.info("Pushing case note {} to event topic with event type of {}", caseNote.id, cne.eventType)
      try {
        val publishResponse = eventTopic.publish(
          cne.eventType,
          jsonMapper.writeValueAsString(cne),
          attributes = mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(cne.eventType).build(),
            "contentType" to MessageAttributeValue.builder().dataType("String")
              .stringValue("text/plain;charset=UTF-8").build(),
            "caseNoteType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(cne.additionalInformation.caseNoteType).build(),
          ),
        )
        log.debug("Sent case note with message id {}", publishResponse.messageId())
      } catch (throwable: Throwable) {
        log.error("Failed to send case note", throwable)
      }
    }
  }
}

data class CaseNoteAdditionalInformation(
  val caseNoteId: String,
  val caseNoteType: String,
) : AdditionalInformation

private fun buildDomainEvent(caseNote: CaseNote, baseUrl: String): DomainEvent<CaseNoteAdditionalInformation> = DomainEvent(
  version = 1,
  eventType = "prison.case-note.published",
  description = "A prison case note has been created or amended",
  detailUrl = URI.create("$baseUrl/case-notes/${caseNote.personIdentifier}/${caseNote.id}").toString(),
  occurredAt = caseNote.createdAt.atZone(EuropeLondon),
  personReference = PersonReference.withIdentifier(caseNote.personIdentifier),
  additionalInformation = CaseNoteAdditionalInformation(
    caseNoteId = caseNote.id,
    caseNoteType = "${caseNote.type}-${caseNote.subType}",
  ),
)
