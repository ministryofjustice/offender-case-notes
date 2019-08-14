package uk.gov.justice.hmpps.casenotes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@Service(value = "auditorAware")
public class AuditorAwareImpl implements AuditorAware<String> {
    private final SecurityUserContext authenticationFacade;

    public AuditorAwareImpl(final SecurityUserContext authenticationFacade) {
        this.authenticationFacade = authenticationFacade;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(authenticationFacade.getCurrentUsername());
    }
}
