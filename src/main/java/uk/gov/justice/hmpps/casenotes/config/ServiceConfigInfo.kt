package uk.gov.justice.hmpps.casenotes.config

import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
final class ServiceConfigInfo : InfoContributor {
  override fun contribute(builder: Info.Builder) {
    builder.withDetail("activeAgencies", listOf("***"))
  }
}

@ConfigurationProperties(prefix = "service")
data class ServiceConfig(
  val baseUrl: String,
  val actionMissingCaseNotes: Boolean,
  val sarEnableAllCaseNotes: Boolean,
) {
  fun switchesPathFor(caseloadId: String?): Boolean = !caseloadId.isNullOrBlank()
}
