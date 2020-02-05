package uk.gov.justice.hmpps.casenotes.health

import com.amazonaws.services.sqs.AmazonSQS
import org.junit.Before
import org.junit.ClassRule
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2MockServer
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthMockServer

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTest {
  @Suppress("unused")
  @Autowired
  lateinit var restTemplate: TestRestTemplate

  @SpyBean
  @Qualifier("awsSqsClient")
  protected lateinit var awsSqsClient: AmazonSQS

  @Value("\${token}")
  private val token: String? = null

  companion object {
    @get:ClassRule
    @JvmStatic
    val oauthMockServer = OAuthMockServer()

    @get:ClassRule
    @JvmStatic
    val elite2MockServer = Elite2MockServer()

  }

  init {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("user", "pw")
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @Before
  fun resetStubs() {
    oauthMockServer.resetAll()
    elite2MockServer.resetAll()

    oauthMockServer.stubGrantToken()
  }

  internal fun createHeaderEntity(entity: Any): HttpEntity<*> {
    val headers = HttpHeaders()
    headers.add("Authorization", "bearer $token")
    headers.contentType = MediaType.APPLICATION_JSON
    return HttpEntity(entity, headers)
  }
}
