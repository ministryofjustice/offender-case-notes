package uk.gov.justice.hmpps.casenotes.health

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component

/**
 * Adds version data to the /health endpoint. This is called by the UI to display API details
 */
@Component
class HealthInfo(@Autowired(required = false) private val buildProperties: BuildProperties?) : HealthIndicator {
  override fun health(): Health = Health.up()
    .withDetail("version", buildProperties?.version ?: "version not available")
    .build()
}
