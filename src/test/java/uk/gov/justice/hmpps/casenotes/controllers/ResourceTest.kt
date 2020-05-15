package uk.gov.justice.hmpps.casenotes.controllers

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension
import uk.gov.justice.hmpps.casenotes.health.wiremock.TokenVerificationExtension
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthHelper
import java.util.function.Consumer

@DirtiesContext(classMode = BEFORE_CLASS)
@ActiveProfiles("test", "noqueue", "token-verification")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ExtendWith(Elite2Extension::class, OAuthExtension::class, TokenVerificationExtension::class)
abstract class ResourceTest {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  internal lateinit var webTestClient: WebTestClient

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  fun addBearerAuthorisation(user: String, roles: List<String> = listOf()): Consumer<HttpHeaders> {
    val jwt = jwtHelper.createJwt(user, roles = roles)
    return addBearerToken(jwt)
  }

  fun addBearerToken(token: String): Consumer<HttpHeaders> = Consumer { headers: HttpHeaders ->
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
    headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
  }

  fun readFile(file: String): String = this.javaClass.getResource(file).readText()
}
