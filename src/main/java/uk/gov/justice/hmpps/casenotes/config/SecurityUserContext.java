package uk.gov.justice.hmpps.casenotes.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.casenotes.config.UserIdAuthenticationConverter.UserIdUser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        return getOptionalCurrentUser().map(User::getUsername);
    }

    public UserIdUser getCurrentUser() {
        return getOptionalCurrentUser().orElseThrow(() -> new IllegalStateException("Current user not set but is required"));
    }

    private Optional<UserIdUser> getOptionalCurrentUser() {
        final var authentication = getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) return Optional.empty();

        final var userPrincipal = authentication.getPrincipal();

        if (userPrincipal instanceof UserIdUser) return Optional.of((UserIdUser) userPrincipal);

        final String username;
        if (userPrincipal instanceof String) {
            username = (String) userPrincipal;
        } else if (userPrincipal instanceof UserDetails) {
            username = ((UserDetails) userPrincipal).getUsername();
        } else if (userPrincipal instanceof Map) {
            final var userPrincipalMap = (Map) userPrincipal;
            username = (String) userPrincipalMap.get("username");
        } else {
            username = userPrincipal.toString();
        }

        if (StringUtils.isEmpty(username) || username.equals("anonymousUser")) return Optional.empty();

        log.debug("Authentication doesn't contain user id, using username instead");
        return Optional.of(new UserIdUser(username, authentication.getCredentials().toString(), authentication.getAuthorities(), username));
    }

    public boolean isOverrideRole(final String... overrideRoles) {
        final var roles = Arrays.asList(overrideRoles.length > 0 ? overrideRoles : new String[]{"SYSTEM_USER"});
        return hasMatchingRole(roles, getAuthentication());
    }
}
