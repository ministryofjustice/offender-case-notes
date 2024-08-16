package uk.gov.justice.hmpps.casenotes.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

abstract class HealthCheck(private val webClient: WebClient) : HealthIndicator {
  override fun health(): Health? =
    webClient.get()
      .uri("/health/ping")
      .retrieve()
      .toEntity(String::class.java)
      .flatMap { Mono.just(Health.up().withDetail("HttpStatus", it?.statusCode).build()) }
      .onErrorResume(WebClientResponseException::class.java) {
        Mono.just(
          Health.down(it).withDetail("body", it.responseBodyAsString).withDetail("HttpStatus", it.statusCode).build(),
        )
      }
      .onErrorResume(Exception::class.java) { Mono.just(Health.down(it).build()) }
      .block()
}

@Component
class Elite2ApiHealth(@Qualifier("elite2ApiHealthWebClient") webClient: WebClient) : HealthCheck(webClient)

@Component
class OAuthApiHealth(@Qualifier("oauthApiHealthWebClient") webClient: WebClient) : HealthCheck(webClient)

@Component
class TokenVerificationApiHealth(
  @Qualifier("tokenVerificationApiHealthWebClient") webClient: WebClient,
  @Value("\${tokenverification.enabled:false}") private val tokenVerificationEnabled: Boolean,
) : HealthCheck(webClient) {
  override fun health(): Health? =
    if (tokenVerificationEnabled) {
      super.health()
    } else {
      Health.up().withDetail("TokenVerification", "Disabled").build()
    }
}
