package uk.gov.justice.hmpps.casenotes.health

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.hmpps.casenotes.config.container.LocalStackContainer
import uk.gov.justice.hmpps.casenotes.config.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.hmpps.casenotes.config.container.PostgresContainer
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension
import uk.gov.justice.hmpps.casenotes.health.wiremock.ManageUsersApiExtension
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension
import uk.gov.justice.hmpps.casenotes.health.wiremock.PrisonerSearchApiExtension
import uk.gov.justice.hmpps.casenotes.health.wiremock.TokenVerificationExtension

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(
    Elite2Extension::class,
    OAuthExtension::class,
    TokenVerificationExtension::class,
    PrisonerSearchApiExtension::class,
    ManageUsersApiExtension::class,
)
abstract class BasicIntegrationTest {
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Value("\${token}")
  private val token: String? = null

  init {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("user", "pw")
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  companion object {
    private val pgContainer = PostgresContainer.instance
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
      }

      System.setProperty("aws.region", "eu-west-2")

      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }
}
