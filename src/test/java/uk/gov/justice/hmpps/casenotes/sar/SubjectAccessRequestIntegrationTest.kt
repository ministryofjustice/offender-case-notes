package uk.gov.justice.hmpps.casenotes.sar

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import javax.sql.DataSource

@Import(SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestIntegrationTest :
  IntegrationTest(),
  SarFlywaySchemaTest,
  SarJpaEntitiesTest,
  SarApiDataTest,
  SarReportTest {
  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getDataSourceInstance(): DataSource = dataSource
  override fun getEntityManagerInstance(): EntityManager = entityManager
  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  override fun setupTestData() {}

  override fun `SAR API should return expected data`() {
    super.`SAR API should return expected data`()
  }
}
