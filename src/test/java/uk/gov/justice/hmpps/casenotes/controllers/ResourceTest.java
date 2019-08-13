package uk.gov.justice.hmpps.casenotes.controllers;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.justice.hmpps.casenotes.integration.wiremock.Elite2MockServer;
import uk.gov.justice.hmpps.casenotes.integration.wiremock.OauthMockServer;

import java.util.Collections;
import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@ActiveProfiles(value = "test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
public abstract class ResourceTest {

    @Autowired
    protected TestRestTemplate testRestTemplate;

    @Rule
    public Elite2MockServer elite2MockServer = new Elite2MockServer();

    @Rule
    public OauthMockServer oauthMockServer = new OauthMockServer();

    HttpEntity<?> createHttpEntity(final String bearerToken, final Object body) {
        return createHttpEntity(bearerToken, body, Collections.emptyMap());
    }

    private HttpEntity<?> createHttpEntity(final String bearerToken, final Object body, final Map<String, String> additionalHeaders) {
        final var headers = new HttpHeaders();

        headers.add("Authorization", "Bearer " + bearerToken);
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");

        additionalHeaders.forEach(headers::add);

        return new HttpEntity<>(body, headers);
    }
}

