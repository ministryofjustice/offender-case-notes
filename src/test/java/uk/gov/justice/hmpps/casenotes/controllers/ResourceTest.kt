package uk.gov.justice.hmpps.casenotes.controllers

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.uuid.Generators
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.ParentTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent
import uk.gov.justice.hmpps.casenotes.health.IntegrationTest
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension
import uk.gov.justice.hmpps.casenotes.health.wiremock.PrisonerSearchApiExtension
import uk.gov.justice.hmpps.casenotes.health.wiremock.TokenVerificationExtension
import uk.gov.justice.hmpps.casenotes.services.MessageAttributes
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthHelper
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import uk.gov.justice.hmpps.casenotes.utils.setByName
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.LocalDateTime
import java.util.UUID
import java.util.function.Consumer

internal const val ACTIVE_PRISON = "MDI"
internal const val USERNAME = "TestUser"

@ActiveProfiles("test", "noqueue", "token-verification")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ExtendWith(
  Elite2Extension::class,
  OAuthExtension::class,
  TokenVerificationExtension::class,
  PrisonerSearchApiExtension::class,
)
abstract class ResourceTest : IntegrationTest() {

  @Autowired
  private lateinit var parentTypeRepository: ParentTypeRepository

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  @Autowired
  internal lateinit var noteRepository: NoteRepository

  @Autowired
  internal lateinit var objectMapper: ObjectMapper

  @Autowired
  internal lateinit var hmppsQueueService: HmppsQueueService

  internal val hmppsEventsQueue by lazy {
    hmppsQueueService.findByQueueId("hmppseventtestqueue")
      ?: throw MissingQueueException("hmppseventtestqueue queue not found")
  }

  private fun HmppsQueue.countAllMessagesOnQueue() = sqsClient.countAllMessagesOnQueue(queueUrl).get()

  fun HmppsQueue.receiveDomainEventsOnQueue(maxMessages: Int = 10): List<DomainEvent> {
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { (it ?: 0) > 0 }
    return sqsClient.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(maxMessages).build(),
    ).get().messages()
      .map { objectMapper.readValue<Notification>(it.body()) }
      .filter { e -> e.eventType in PersonCaseNoteEvent.Type.entries.map { "person.case-note.${it.name.lowercase()}" } }
      .map { objectMapper.readValue<DomainEvent>(it.message) }
  }

  fun HmppsQueue.receiveDomainEvent(): DomainEvent {
    val event = receiveDomainEventsOnQueue().single()
    sqsClient.purgeQueue { it.queueUrl(queueUrl) }
    return event
  }

  private data class Notification(
    @JsonProperty("Message") val message: String,
    @JsonProperty("MessageAttributes") val attributes: MessageAttributes = MessageAttributes(),
  ) {
    val eventType: String? @JsonIgnore get() = attributes["eventType"]?.value
  }

  private data class MessageAttributes(
    @JsonAnyGetter @JsonAnySetter
    private val attributes: MutableMap<String, MessageAttribute> = mutableMapOf(),
  ) : MutableMap<String, MessageAttribute> by attributes {
    override operator fun get(key: String): MessageAttribute? = attributes[key]
    operator fun set(key: String, value: MessageAttribute) {
      attributes[key] = value
    }
  }

  private data class MessageAttribute(@JsonProperty("Type") val type: String, @JsonProperty("Value") val value: String)

  fun addBearerAuthorisation(user: String, roles: List<String> = listOf()): Consumer<HttpHeaders> {
    val jwt = jwtHelper.createJwt(user, roles = roles)
    return addBearerToken(jwt)
  }

  fun addBearerToken(token: String): Consumer<HttpHeaders> = Consumer { headers: HttpHeaders ->
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
    headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
  }

  fun RequestBodySpec.addHeader(key: String, value: String? = null) = apply {
    value?.also { header(key, it) }
  }

  fun readFile(file: String): String = this.javaClass.getResource(file)!!.readText()

  internal final fun WebTestClient.ResponseSpec.errorResponse(status: HttpStatus): ErrorResponse =
    expectStatus().isEqualTo(status)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

  internal final inline fun <reified T> WebTestClient.ResponseSpec.success(status: HttpStatus = HttpStatus.OK): T =
    expectStatus().isEqualTo(status)
      .expectBody(T::class.java)
      .returnResult().responseBody!!

  internal final inline fun <reified T> WebTestClient.ResponseSpec.successList(status: HttpStatus = HttpStatus.OK): List<T> =
    expectStatus().isEqualTo(status)
      .expectBodyList(T::class.java)
      .returnResult().responseBody!!

  fun getAllTypes(
    includeInactive: Boolean = true,
    includeRestricted: Boolean = true,
    dpsSelectableOnly: Boolean = false,
  ): List<SubType> =
    parentTypeRepository.findAllWithParams(
      includeInactive = true,
      includeRestricted = true,
      dpsUserSelectableOnly = false,
    ).flatMap { it.getSubtypes() }

  fun givenRandomType(active: Boolean? = null, sensitive: Boolean? = null, restricted: Boolean? = null): SubType =
    getAllTypes().filter {
      (active == null || it.active == active) &&
        (sensitive == null || it.sensitive == sensitive) &&
        (restricted == null || it.restrictedUse == restricted)
    }.random()

  fun generateCaseNote(
    prisonNumber: String = NomisIdGenerator.prisonNumber(),
    type: SubType = givenRandomType(),
    occurredAt: LocalDateTime = LocalDateTime.now().minusDays(3),
    locationId: String = "MDI",
    authorUsername: String = "AuthorUsername",
    authorUserId: String = "AuthorId",
    authorName: String = "Author Name",
    text: String = "Text about the case note saved in the case note database",
    systemGenerated: Boolean = false,
    legacyId: Long = NomisIdGenerator.newId(),
    createdAt: LocalDateTime? = null,
  ) = Note(
    prisonNumber,
    type,
    occurredAt,
    locationId,
    authorUsername,
    authorUserId,
    authorName,
    text,
    systemGenerated,
  ).apply {
    this.legacyId = legacyId
    createdAt?.also { this.createdAt = it }
  }

  fun Note.withAmendment(
    authorUsername: String = "AuthorUsername",
    authorUserId: String = "AuthorId",
    authorName: String = "Author Name",
    text: String = "An amendment to a case note saved in the case note database",
    createdAt: LocalDateTime = LocalDateTime.now(),
    id: UUID = Generators.timeBasedEpochGenerator().generate(),
  ): Note = apply {
    val amendment =
      Amendment(this, authorUsername, authorName, authorUserId, text, id).apply { this.createdAt = createdAt }
    setByName("amendments", (amendments() + amendment).toSortedSet())
  }

  fun givenCaseNote(note: Note): Note = noteRepository.save(note)
}
