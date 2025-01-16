package uk.gov.justice.hmpps.casenotes.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.uuid.Generators
import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.hmpps.casenotes.domain.System
import uk.gov.justice.hmpps.casenotes.events.CaseNoteInformation
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.Notification
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent
import uk.gov.justice.hmpps.casenotes.health.BasicIntegrationTest
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension
import uk.gov.justice.hmpps.casenotes.health.wiremock.ManageUsersApiExtension
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension
import uk.gov.justice.hmpps.casenotes.health.wiremock.PrisonerSearchApiExtension
import uk.gov.justice.hmpps.casenotes.health.wiremock.TokenVerificationExtension
import uk.gov.justice.hmpps.casenotes.legacy.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthHelper
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import uk.gov.justice.hmpps.casenotes.utils.setByName
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import uk.gov.justice.hmpps.sqs.publish
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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
  ManageUsersApiExtension::class,
)
abstract class IntegrationTest : BasicIntegrationTest() {

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

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents") ?: throw MissingTopicException("domain events topic not found")
  }

  val domainEventsQueue by lazy {
    hmppsQueueService.findByQueueId("domaineventsqueue") ?: throw MissingQueueException("domain events queue not found")
  }

  internal fun publishEventToTopic(event: DomainEvent<*>) {
    domainEventsTopic.publish(event.eventType, objectMapper.writeValueAsString(event))
  }

  internal fun HmppsQueue.countAllMessagesOnQueue() = sqsClient.countAllMessagesOnQueue(queueUrl).get()

  internal fun HmppsQueue.receivePersonCaseNoteEventsOnQueue(maxMessages: Int = 10): List<DomainEvent<CaseNoteInformation>> {
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { (it ?: 0) > 0 }
    return sqsClient.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(maxMessages).build(),
    ).get().messages()
      .map { objectMapper.readValue<Notification>(it.body()) }
      .filter { e -> e.eventType in PersonCaseNoteEvent.Type.entries.map { "person.case-note.${it.name.lowercase()}" } }
      .map {
        val event = objectMapper.readValue<DomainEvent<CaseNoteInformation>>(it.message)
        assertThat(it.attributes["type"]?.value).isEqualTo(event.additionalInformation.type)
        assertThat(it.attributes["subType"]?.value).isEqualTo(event.additionalInformation.subType)
        event
      }
  }

  internal fun HmppsQueue.receivePersonCaseNoteEvent(): DomainEvent<CaseNoteInformation> {
    val event = receivePersonCaseNoteEventsOnQueue().single()
    sqsClient.purgeQueue { it.queueUrl(queueUrl) }
    return event
  }

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
    prisonNumber: String = NomisIdGenerator.personIdentifier(),
    type: SubType = givenRandomType(),
    occurredAt: LocalDateTime = LocalDateTime.now().minusDays(3),
    locationId: String = "MDI",
    authorUsername: String = "AuthorUsername",
    authorUserId: String = "AuthorId",
    authorName: String = "Author Name",
    text: String = "Text about the case note saved in the case note database",
    systemGenerated: Boolean = false,
    system: System = System.DPS,
    legacyId: Long = NomisIdGenerator.newId(),
    createdAt: LocalDateTime? = null,
  ) = Note(
    prisonNumber,
    type,
    occurredAt.truncatedTo(ChronoUnit.SECONDS),
    locationId,
    authorUsername,
    authorUserId,
    authorName,
    text,
    systemGenerated,
    system,
  ).apply {
    this.legacyId = legacyId
    createdAt?.also { this.createdAt = it }
  }

  fun Note.withAmendment(
    authorUsername: String = "AuthorUsername",
    authorUserId: String = "AuthorId",
    authorName: String = "Author Name",
    text: String = "An amendment to a case note saved in the case note database",
    system: System = System.DPS,
    createdAt: LocalDateTime = LocalDateTime.now(),
    id: UUID = Generators.timeBasedEpochGenerator().generate(),
  ): Note = apply {
    val amendment =
      Amendment(this, authorUsername, authorName, authorUserId, text, system, id).apply { this.createdAt = createdAt }
    setByName("amendments", (amendments() + amendment).toSortedSet())
  }

  fun givenCaseNote(note: Note): Note = noteRepository.save(note)
}
