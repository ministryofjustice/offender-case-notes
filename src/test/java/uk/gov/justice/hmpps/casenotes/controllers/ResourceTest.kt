package uk.gov.justice.hmpps.casenotes.controllers

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.NoteRepository
import uk.gov.justice.hmpps.casenotes.domain.ParentTypeRepository
import uk.gov.justice.hmpps.casenotes.domain.SubType
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.health.IntegrationTest
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension
import uk.gov.justice.hmpps.casenotes.health.wiremock.TokenVerificationExtension
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthHelper
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import uk.gov.justice.hmpps.casenotes.utils.setByName
import java.time.LocalDateTime
import java.util.function.Consumer

internal const val ACTIVE_PRISON = "MDI"
internal const val USERNAME = "TestUser"

@ActiveProfiles("test", "noqueue", "token-verification")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ExtendWith(Elite2Extension::class, OAuthExtension::class, TokenVerificationExtension::class)
abstract class ResourceTest : IntegrationTest() {

  @Autowired
  private lateinit var parentTypeRepository: ParentTypeRepository

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  @Autowired
  internal lateinit var noteRepository: NoteRepository

  fun addBearerAuthorisation(user: String, roles: List<String> = listOf()): Consumer<HttpHeaders> {
    val jwt = jwtHelper.createJwt(user, roles = roles)
    return addBearerToken(jwt)
  }

  fun addBearerToken(token: String): Consumer<HttpHeaders> = Consumer { headers: HttpHeaders ->
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
    headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
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
    authorName: String = "AuthorName",
    text: String = "Text about the case note saved in the case note database",
    systemGenerated: Boolean = false,
    legacyId: Long? = null,
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
    new = true
    this.legacyId = legacyId
    createdAt?.also { createDateTime = it }
  }

  fun Note.withAmendment(
    authorUsername: String = "AuthorUsername",
    authorUserId: String = "AuthorId",
    authorName: String = "AuthorName",
    text: String = "An amendment to a case note saved in the case note database",
    id: Long? = null,
  ): Note = apply {
    val amendment = Amendment(this, authorUsername, authorName, authorUserId, text, id)
    setByName("amendments", (amendments() + amendment).toSortedSet())
  }

  fun givenCaseNote(note: Note): Note = noteRepository.save(note)
}
