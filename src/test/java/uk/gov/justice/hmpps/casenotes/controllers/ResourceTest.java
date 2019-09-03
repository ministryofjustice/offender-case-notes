package uk.gov.justice.hmpps.casenotes.controllers;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.justice.hmpps.casenotes.dto.CaseNote;
import uk.gov.justice.hmpps.casenotes.integration.wiremock.Elite2MockServer;
import uk.gov.justice.hmpps.casenotes.integration.wiremock.OauthMockServer;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.core.ResolvableType.forType;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@ActiveProfiles(value = "test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
public abstract class ResourceTest {

    @Autowired
    protected TestRestTemplate testRestTemplate;

    @ClassRule
    public static final Elite2MockServer elite2MockServer = new Elite2MockServer();

    @ClassRule
    public static final OauthMockServer oauthMockServer = new OauthMockServer();

    @Before
    public void resetStubs() {
        elite2MockServer.resetAll();
        oauthMockServer.resetAll();
    }

    HttpEntity<?> createHttpEntity(final String bearerToken, final Object body) {
        final var headers = new HttpHeaders();

        headers.add(AUTHORIZATION, "Bearer " + bearerToken);
        headers.add(ACCEPT, APPLICATION_JSON_VALUE);

        if (body != null) headers.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);

        return new HttpEntity<>(body, headers);
    }

    <T> void assertJsonAndStatus(final ResponseEntity<String> response, final Class<T> type, final int status, final String jsonFile) {
        assertThat(response.getStatusCodeValue()).withFailMessage("Expecting status code value <%s> to be equal to <%s> but it was not.\nBody was\n%s", response.getStatusCodeValue(), status, response.getBody()).isEqualTo(status);

        assertThat(getBodyAsJsonContent(type, response)).isEqualToJson(jsonFile);
    }

    private <T> JsonContent<CaseNote> getBodyAsJsonContent(final Class<T> type, final ResponseEntity<String> response) {
        return new JsonContent<>(getClass(), forType(type), Objects.requireNonNull(response.getBody()));
    }

}

