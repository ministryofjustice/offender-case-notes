package uk.gov.justice.hmpps.casenotes.sar

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthHelper
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.Duration

@Import(SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestIntegrationTest :
  IntegrationTest(),
  SarJpaEntitiesTest {

  @TestConfiguration
  class Config {
    @Autowired
    lateinit var jwtHelper: JwtAuthHelper

    @Primary
    @Bean
    fun jwtAuthorisationHelper() = object : JwtAuthorisationHelper() {
      override fun jwtDecoder(): JwtDecoder = jwtHelper.jwtDecoder()

      override fun createJwtAccessToken(
        clientId: String,
        username: String?,
        scope: List<String>?,
        roles: List<String>?,
        expiryTime: Duration,
        jwtId: String,
        authSource: String,
        grantType: String,
      ): String = jwtHelper.createJwt(
        subject = username ?: clientId,
        userId = username,
        scope = scope,
        roles = roles,
      )
    }
  }

  @Autowired
  private lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  override fun getEntityManagerInstance(): EntityManager = entityManager
  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper
}
