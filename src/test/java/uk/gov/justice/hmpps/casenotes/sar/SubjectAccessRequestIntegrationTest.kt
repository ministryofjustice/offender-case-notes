package uk.gov.justice.hmpps.casenotes.sar

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationState
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthHelper
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.Duration
import javax.sql.DataSource

@Import(SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestIntegrationTest :
  IntegrationTest(),
  SarFlywaySchemaTest,
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
  private lateinit var dataSource: DataSource

  @Autowired
  private lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  override fun getDataSourceInstance(): DataSource = dataSource
  override fun getEntityManagerInstance(): EntityManager = entityManager
  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  @Test
  fun `Flyway schema version should match expected non-future version`(
    @Value($$"${hmpps.sar.tests.expected-flyway-schema-non-future-version:0}")
    expectedFlywaySchemaNonFutureVersion: String,
  ) {
    // NB: SAR testing library includes test-only migrations so this test case exists to compare main schema only
    val migrations = Flyway.configure().dataSource(dataSource).load().info().all()
    val lastMigration = migrations.findLast {
      it.state == MigrationState.SUCCESS
    } ?: fail("Last successful non-future migration not found")
    assertThat(lastMigration.version?.version).`as`("Flyway schema version").isEqualTo(expectedFlywaySchemaNonFutureVersion)
  }
}
