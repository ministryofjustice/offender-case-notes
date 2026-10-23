package uk.gov.justice.hmpps.casenotes.sar

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
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
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SubjectAccessRequestResponse
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthHelper
import uk.gov.justice.hmpps.kotlin.auth.AuthSource
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import javax.sql.DataSource

@Import(SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestIntegrationTest :
  IntegrationTest(),
  SarApiDataTest,
  SarFlywaySchemaTest,
  SarJpaEntitiesTest,
  SarReportTest {
  @Autowired
  private lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  private lateinit var objectMapper: ObjectMapper

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
  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  private val samplePrisonerNumber = "A1234BB"

  override fun setupTestData() {
    val types = getAllTypes()
    var date = LocalDateTime.parse("2025-04-22T13:17:53")
    noteRepository.findByLegacyId(104) ?: givenCaseNote(
      generateCaseNote(
        personIdentifier = samplePrisonerNumber,
        type = types.find { it.type.code == "POM" && it.code == "GEN" }!!,
        text = "Notes about something that happened the day before",
        authorUsername = "POM1",
        authorUserId = "3124",
        legacyId = 104,
        occurredAt = date.minusDays(1).minusHours(1).minusMinutes(10),
        createdAt = date,
        locationId = "LEI",
      ).withAmendment(
        text = "An amendment about what happened.",
        authorUsername = "POM2",
        createdAt = date.plusDays(1).plusHours(1),
      ),
    )
    date = LocalDateTime.parse("2025-04-20T08:32:06")
    noteRepository.findByLegacyId(103) ?: givenCaseNote(
      generateCaseNote(
        personIdentifier = samplePrisonerNumber,
        type = types.find { it.type.code == "OMIC" && it.code == "GEN" }!!,
        text = "OMIC notes\nWith my comments",
        authorUsername = "OMIC1",
        authorUserId = "3124",
        legacyId = 103,
        occurredAt = date.minusHours(5),
        createdAt = date,
        locationId = "LEI",
      ),
    )
    date = LocalDateTime.parse("2025-01-13T18:24:32")
    noteRepository.findByLegacyId(102) ?: givenCaseNote(
      generateCaseNote(
        personIdentifier = samplePrisonerNumber,
        type = types.find { it.type.code == "NOMIS_SYNC" && it.code == "SYNC" }!!,
        text = "A note that is sync'd to NOMIS",
        authorUsername = "NOMIS_SYNC",
        authorUserId = "3130",
        legacyId = 102,
        occurredAt = date.minusHours(1),
        createdAt = date,
        locationId = "LEI",
      ).withAmendment(
        text = "That’s not quite right",
        authorUsername = "NOMIS_SYNC_2",
        createdAt = date,
      ),
    )
    givenCaseNote(generateCaseNote())
  }

  override fun getPrn(): String? = samplePrisonerNumber

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
    assertThat(lastMigration.version?.version).`as`("Flyway schema version")
      .isEqualTo(expectedFlywaySchemaNonFutureVersion)
  }

  // TODO - Delete this
  @Test
  fun `SAR API should return expected data - With filter`() {
    setupTestData()

    val response = getSarDataWithFilter(
      getPrn(),
      getCrn(),
      getFromDate(),
      getToDate(),
      getWebTestClientInstance(),
      getContentType(),
      getAuthorisationSource(),
    )
    if (System.getenv("SAR_GENERATE_ACTUAL").toBoolean()) {
      getSarHelper().saveSarApiResponse(response)
    } else {
      assertThatJson(getSarHelper().toJson(response)).`as`("Response content json")
        .isEqualTo(
          this::class.java.getResource("/sar/tmp/expected-api-response.json")?.readText()!!,
        )
      assertThat(response.attachments?.isNotEmpty() == true).`as`("Response has attachments")
        .isEqualTo(getSarHelper().attachmentsExpected)
    }
  }

  // TODO - Delete this
  @Test
  fun `SAR report should render as expected - With filter`() {
    setupTestData()
    getSarHelper().stubFindPrisonNameWith("Moorland (HMP & YOI)")
    getSarHelper().stubFindUserLastNameWith("Johnson")
    getSarHelper().stubFindLocationNameByNomisIdWith("PROPERTY BOX 1")
    getSarHelper().stubFindLocationNameByDpsIdWith("PROPERTY BOX 2")
    getInlineAttachments().forEach { attachment ->
      getSarHelper().stubGetAttachment(attachment.key, getSarHelper().getResourceAsBytes(attachment.value))
    }
    val dataResponse = getSarDataWithFilter(
      getPrn(),
      getCrn(),
      getFromDate(),
      getToDate(),
      getWebTestClientInstance(),
      getContentType(),
      getAuthorisationSource(),
    )
    val templateResponse = getSarHelper().requestSarTemplate(getWebTestClientInstance(), getAuthorisationSource())

    val renderResult = getSarHelper().renderServiceReport(
      data = dataResponse.content,
      templateVersion = "1.0",
      template = templateResponse,
    )

    getSarHelper().renderAndSaveReportAsPdf(renderResult, getPrn(), getCrn())

    if (System.getenv("SAR_GENERATE_ACTUAL").toBoolean()) {
      getSarHelper().saveGeneratedReport(renderResult)
    } else {
      getSarHelper().assertHtmlEquals(
        renderResult,
        this::class.java.getResource("/sar/tmp/expected-render-result.html")?.readText()!!,
      )
    }
  }

  private fun <T> getSarDataWithFilter(
    prn: String?,
    crn: String?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    webTestClient: WebTestClient,
    contentType: Class<T>,
    authSource: AuthSource = AuthSource.NONE,
  ): SubjectAccessRequestResponse<T> {
    val response = webTestClient.get().uri {
      it.path("/subject-access-request")
        .queryParamIfPresent("prn", Optional.ofNullable(prn))
        .queryParamIfPresent("crn", Optional.ofNullable(crn))
        .queryParamIfPresent("fromDate", Optional.ofNullable(fromDate))
        .queryParamIfPresent("toDate", Optional.ofNullable(toDate))
        .queryParamIfPresent("filter", Optional.ofNullable("omic"))
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS"), authSource = authSource))
      .exchange()
      .expectStatus().isOk
      .expectBody(String::class.java)
      .returnResult().responseBody!!

    return objectMapper.readValue(
      response,
      objectMapper.typeFactory.constructParametricType(SubjectAccessRequestResponse::class.java, contentType),
    )
  }

  fun setAuthorisation(
    username: String? = "TEST_USR",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
    authSource: AuthSource,
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles, authSource = authSource)
}
