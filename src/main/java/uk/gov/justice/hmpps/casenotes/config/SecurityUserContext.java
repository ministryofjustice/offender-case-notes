package uk.gov.justice.hmpps.casenotes.config;

import org.apache.commons.lang3.RegExUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SecurityUserContext {

	private Authentication getAuthentication() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	public String getCurrentUsername() {
		String username;

		Object userPrincipal = getUserPrincipal();

		if (userPrincipal instanceof String) {
			username = (String) userPrincipal;
		} else if (userPrincipal instanceof UserDetails) {
			username = ((UserDetails)userPrincipal).getUsername();
		} else if (userPrincipal instanceof Map) {
			Map userPrincipalMap = (Map) userPrincipal;
			username = (String) userPrincipalMap.get("username");
		} else {
			username = null;
		}

		return username;
	}

	private Object getUserPrincipal() {
		Object userPrincipal = null;

		final Authentication auth = getAuthentication();

		if (auth != null) {
			userPrincipal = auth.getPrincipal();
		}
		return userPrincipal;
	}

	public static boolean hasRoles(final String... allowedRoles) {
		final var roles = Arrays.stream(allowedRoles)
				.map(r -> RegExUtils.replaceFirst(r, "ROLE_", ""))
				.collect(Collectors.toList());

		return hasMatchingRole(roles, SecurityContextHolder.getContext().getAuthentication());
	}

	public boolean isOverrideRole(final String... overrideRoles) {
		final var roles = Arrays.asList(overrideRoles.length > 0 ? overrideRoles : new String[]{"SYSTEM_USER"});
		return hasMatchingRole(roles, getAuthentication());
	}

	private static boolean hasMatchingRole(final List<String> roles, final Authentication authentication) {
		return authentication != null &&
				authentication.getAuthorities().stream()
						.anyMatch(a -> roles.contains(RegExUtils.replaceFirst(a.getAuthority(), "ROLE_", "")));
	}
}
