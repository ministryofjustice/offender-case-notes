package uk.gov.justice.hmpps.casenotes.health

import lombok.AccessLevel
import lombok.AllArgsConstructor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@AllArgsConstructor(access = AccessLevel.PROTECTED)
abstract class HealthCheck(private val restTemplate: RestTemplate) : HealthIndicator {

  override fun health(): Health {
    return try {
      val responseEntity = restTemplate.getForEntity("/ping", String::class.java)
      Health.up().withDetail("HttpStatus", responseEntity.statusCode).build()
    } catch (e: RestClientException) {
      Health.down(e).build()
    }
  }
}

@Component
class Elite2ApiHealth
constructor(@Qualifier("elite2ApiHealthRestTemplate") restTemplate: RestTemplate) : HealthCheck(restTemplate)

@Component
class OAuthApiHealth
constructor(@Qualifier("oauthApiHealthRestTemplate") restTemplate: RestTemplate) : HealthCheck(restTemplate)

