package uk.gov.justice.hmpps.casenotes.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

@Slf4j
@Component
@Order(1)
public class UserMdcFilter implements Filter {
    private static final String USER_ID_HEADER = "userId";

    private final SecurityUserContext securityUserContext;

    @Autowired
    public UserMdcFilter(final SecurityUserContext securityUserContext) {
        this.securityUserContext = securityUserContext;
    }

    @Override
    public void init(final FilterConfig filterConfig) {
        // Initialise - no functionality
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        final var currentUsername = securityUserContext.getCurrentUsername();

        try {
            currentUsername.ifPresent(u -> MDC.put(USER_ID_HEADER, u));
            chain.doFilter(request, response);
        } finally {
            currentUsername.ifPresent(u -> MDC.remove(USER_ID_HEADER));
        }
    }

    @Override
    public void destroy() {
        // Destroy - no functionality
    }
}
