package uk.gov.justice.hmpps.casenotes.config;

import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;

import java.util.Collection;
import java.util.Map;

public class UserIdAuthenticationConverter extends DefaultUserAuthenticationConverter {
    @Override
    public Authentication extractAuthentication(final Map<String, ?> map) {
        final var parentAuth = super.extractAuthentication(map);

        final var user = new UserIdUser(parentAuth.getName(), parentAuth.getCredentials().toString(), parentAuth.getAuthorities(), (String) map.get("user_id"));

        return new UsernamePasswordAuthenticationToken(user, parentAuth.getCredentials(), parentAuth.getAuthorities());
    }

    @Getter
    public static final class UserIdUser extends User {
        private final String userId;

        public UserIdUser(final String username, final String password, final Collection<? extends GrantedAuthority> authorities, final String userId) {
            super(username, password, authorities);
            this.userId = userId;
        }
    }
}
