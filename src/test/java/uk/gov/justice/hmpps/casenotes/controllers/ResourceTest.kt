package uk.gov.justice.hmpps.casenotes.controllers;

import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension;
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension;
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthenticationHelper;
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthenticationHelper.JwtParameters;
import wiremock.org.apache.commons.io.IOUtils;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ActiveProfiles(value = "test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ExtendWith({Elite2Extension.class, OAuthExtension.class})
public abstract class ResourceTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected JwtAuthenticationHelper jwtAuthenticationHelper;

    Consumer<HttpHeaders> addBearerAuthorisation(final String user, final List<String> roles) {
        final var jwt = createJwt(user, roles);
        return addBearerToken(jwt);
    }

    Consumer<HttpHeaders> addBearerToken(final String token) {
        return headers -> {
            headers.add(AUTHORIZATION, "Bearer " + token);
            headers.add(ACCEPT, APPLICATION_JSON_VALUE);
            headers.add(CONTENT_TYPE, APPLICATION_JSON_VALUE);
        };
    }

    String createJwt(final String user, final List<String> roles) {
        return jwtAuthenticationHelper.createJwt(JwtParameters.builder()
                .username(user)
                .userId(user + "_ID")
                .roles(roles)
                .scope(List.of("read", "write"))
                .expiryTime(Duration.ofDays(1))
                .build());
    }

    @SneakyThrows
    String readFile(final String file) {
        return IOUtils.toString(this.getClass().getResourceAsStream(file));
    }
}

