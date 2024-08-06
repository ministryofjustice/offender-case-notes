package uk.gov.justice.hmpps.casenotes.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.Optional.of

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@Service(value = "auditorAware")
class AuditorAwareImpl : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> = of(CaseNoteRequestContext.get().username)
}
