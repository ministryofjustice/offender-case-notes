package uk.gov.justice.hmpps.casenotes.controllers

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.hmpps.casenotes.health.IntegrationTest
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension
import uk.gov.justice.hmpps.casenotes.health.wiremock.TokenVerificationExtension
import uk.gov.justice.hmpps.casenotes.types.CreateSubType
import uk.gov.justice.hmpps.casenotes.types.internal.ParentType
import uk.gov.justice.hmpps.casenotes.types.internal.ParentTypeRepository
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthHelper
import java.util.function.Consumer

@DirtiesContext(classMode = BEFORE_CLASS)
@ActiveProfiles("test", "noqueue", "token-verification")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ExtendWith(Elite2Extension::class, OAuthExtension::class, TokenVerificationExtension::class)
abstract class ResourceTest : IntegrationTest() {

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  @Autowired
  internal lateinit var parentTypeRepository: ParentTypeRepository

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

  internal final inline fun <reified T> WebTestClient.ResponseSpec.successList(status: HttpStatus = HttpStatus.OK): List<T> =
    expectStatus().isEqualTo(status)
      .expectBodyList(T::class.java)
      .returnResult().responseBody!!

  fun givenParentType(parentTypeCode: String, subTypes: Set<CreateSubType>): ParentType {
    val pt = ParentType("PT1", "Description of PT1")
    subTypes.forEach { pt.addSubType(it) }
    return parentTypeRepository.save(pt)
  }
}
