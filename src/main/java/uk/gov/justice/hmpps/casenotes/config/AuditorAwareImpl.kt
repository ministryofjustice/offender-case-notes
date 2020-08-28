package uk.gov.justice.hmpps.casenotes.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.stereotype.Service
import java.util.*

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@Service(value = "auditorAware")
class AuditorAwareImpl(private val authenticationFacade: SecurityUserContext) : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> = authenticationFacade.getCurrentUsername()
}
