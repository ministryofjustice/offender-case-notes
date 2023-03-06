package uk.gov.justice.hmpps.casenotes.health

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension.Companion.elite2Api
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi
import uk.gov.justice.hmpps.casenotes.health.wiremock.TokenVerificationExtension.Companion.tokenVerificationApi

class HealthCheckIntegrationTest : QueueListenerIntegrationTest() {

  @Test
  fun `Health page reports ok`() {
    subPing(200)

    webTestClient.get().uri("/health").exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.OAuthApiHealth.details.HttpStatus").isEqualTo("OK")
      .jsonPath("components.elite2ApiHealth.details.HttpStatus").isEqualTo("OK")
      .jsonPath("components.tokenVerificationApiHealth.details.TokenVerification").isEqualTo("Disabled")
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Queue health reports queue details`() {
    purgeQueues()
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.event-health.details.queueName").isEqualTo(eventQueueName)
      .jsonPath("components.event-health.details.messagesOnQueue").isEqualTo(0)
      .jsonPath("components.event-health.details.messagesInFlight").isEqualTo(0)
      .jsonPath("components.event-health.details.messagesOnDlq").isEqualTo(0)
      .jsonPath("components.event-health.details.dlqStatus").isEqualTo("UP")
      .jsonPath("components.event-health.details.dlqName").isEqualTo(eventDlqName)
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
      .jsonPath("components.tokenVerificationApiHealth.details.TokenVerification").isEqualTo("Disabled")
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
      .jsonPath("components.tokenVerificationApiHealth.details.TokenVerification").isEqualTo("Disabled")
      .jsonPath("status").isEqualTo("DOWN")
  }

  @Test
  fun `Health liveness page is accessible`() {
    webTestClient.get().uri("/health/liveness")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health readiness page is accessible`() {
    webTestClient.get().uri("/health/readiness")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("status").isEqualTo("UP")
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
