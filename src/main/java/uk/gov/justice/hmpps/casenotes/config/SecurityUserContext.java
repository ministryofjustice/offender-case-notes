package uk.gov.justice.hmpps.casenotes.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class SecurityUserContext {
    private static boolean hasMatchingRole(final List<String> roles, final Authentication authentication) {
        return authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> roles.contains(RegExUtils.replaceFirst(a.getAuthority(), "ROLE_", "")));
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public Optional<String> getCurrentUsername() {
        return getOptionalCurrentUser().map(UserIdUser::getUsername);
    }

    public UserIdUser getCurrentUser() {
        return getOptionalCurrentUser().orElseThrow(() -> new IllegalStateException("Current user not set but is required"));
    }

    private Optional<UserIdUser> getOptionalCurrentUser() {
        final var authentication = getAuthentication();
        if (!(authentication instanceof AuthAwareAuthenticationToken)) return Optional.empty();

        return Optional.of(((AuthAwareAuthenticationToken) authentication).getUserIdUser());
    }

    public boolean isOverrideRole(final String... overrideRoles) {
        final var roles = Arrays.asList(overrideRoles.length > 0 ? overrideRoles : new String[]{"SYSTEM_USER"});
        return hasMatchingRole(roles, getAuthentication());
    }

    @Data
    public static final class UserIdUser {
        private final String username;
        private final String userId;

        public UserIdUser(final String username, final String userId) {
            this.username = username;
            this.userId = userId;
        }
    }
}
