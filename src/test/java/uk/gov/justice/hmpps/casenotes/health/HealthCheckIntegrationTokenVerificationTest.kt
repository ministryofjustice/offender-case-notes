package uk.gov.justice.hmpps.casenotes.health

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension.Companion.elite2Api
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi
import uk.gov.justice.hmpps.casenotes.health.wiremock.TokenVerificationExtension.Companion.tokenVerificationApi

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = ["tokenverification.enabled=true", "sqs.provider=false"],
)
class HealthCheckIntegrationTokenVerificationTest : BasicIntegrationTest() {

  @Test
  fun `Health page reports ok`() {
    subPing(200)

    webTestClient.get().uri("/health").exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.OAuthApiHealth.details.HttpStatus").isEqualTo("OK")
      .jsonPath("components.elite2ApiHealth.details.HttpStatus").isEqualTo("OK")
      .jsonPath("components.prisonerSearchApiHealth.details.HttpStatus").isEqualTo("OK")
      .jsonPath("components.manageUserApiHealth.details.HttpStatus").isEqualTo("OK")
      .jsonPath("components.tokenVerificationApiHealth.details.HttpStatus").isEqualTo("OK")
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health ping page is accessible`() {
    subPing(200)

    webTestClient.get().uri("/health/ping").exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health page reports down`() {
    subPing(404)

    webTestClient.get().uri("/health").exchange()
      .expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("components.OAuthApiHealth.details.error").value<String> {
        assertThat(it).contains("WebClientResponseException\$NotFound: 404 Not Found")
      }
      .jsonPath("components.elite2ApiHealth.details.error").value<String> {
        assertThat(it).contains("WebClientResponseException\$NotFound: 404 Not Found")
      }
      .jsonPath("components.tokenVerificationApiHealth.details.error").value<String> {
        assertThat(it).contains("WebClientResponseException\$NotFound: 404 Not Found")
      }
      .jsonPath("status").isEqualTo("DOWN")
  }

  @Test
  fun `Health page reports a teapot`() {
    subPing(418)

    webTestClient.get().uri("/health").exchange()
      .expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("components.OAuthApiHealth.details.error").value<String> {
        assertThat(it).contains("WebClientResponseException: 418 I'm a teapot")
      }
      .jsonPath("components.elite2ApiHealth.details.error").value<String> {
        assertThat(it).contains("WebClientResponseException: 418 I'm a teapot")
      }
      .jsonPath("components.tokenVerificationApiHealth.details.error").value<String> {
        assertThat(it).contains("WebClientResponseException: 418 I'm a teapot")
      }
      .jsonPath("status").isEqualTo("DOWN")
  }

  private fun subPing(status: Int) {
    oAuthApi.stubFor(
      get("/auth/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "{\"status\":\"UP\"}" else "some error")
          .withStatus(status),
      ),
    )

    elite2Api.stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "{\"status\":\"UP\"}" else "some error")
          .withStatus(status),
      ),
    )

    tokenVerificationApi.stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "{\"status\":\"UP\"}" else "some error")
          .withStatus(status),
      ),
    )
  }
}
