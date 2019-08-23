package uk.gov.justice.hmpps.casenotes.health;

import org.junit.Test;
import uk.gov.justice.hmpps.casenotes.controllers.ResourceTest;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

public class HealthCheckTest extends ResourceTest {
    @Test
    public void testDown() {
        elite2MockServer.subPing(404);
        oauthMockServer.subPing(404);

        final var response = testRestTemplate.getForEntity("/health", String.class);
        assertThatJson(response.getBody()).node("details.elite2ApiHealth.details.error").isEqualTo("org.springframework.web.client.HttpClientErrorException$NotFound: 404 Not Found");
        assertThatJson(response.getBody()).node("details.OAuthApiHealth.details.error").isEqualTo("org.springframework.web.client.HttpClientErrorException$NotFound: 404 Not Found");
        assertThatJson(response.getBody()).node("status").isEqualTo("DOWN");
        assertThat(response.getStatusCodeValue()).isEqualTo(503);
    }

    @Test
    public void testUp() {
        elite2MockServer.subPing(200);
        oauthMockServer.subPing(200);

        final var response = testRestTemplate.getForEntity("/health", String.class);
        assertThatJson(response.getBody()).node("details.elite2ApiHealth.details.HttpStatus").isEqualTo("OK");
        assertThatJson(response.getBody()).node("details.OAuthApiHealth.details.HttpStatus").isEqualTo("OK");
        assertThatJson(response.getBody()).node("status").isEqualTo("UP");
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    public void testTeapot() {
        elite2MockServer.subPing(418);
        oauthMockServer.subPing(418);

        final var response = testRestTemplate.getForEntity("/health", String.class);
        assertThatJson(response.getBody()).node("details.elite2ApiHealth.details.error").isEqualTo("org.springframework.web.client.HttpClientErrorException: 418 418");
        assertThatJson(response.getBody()).node("details.OAuthApiHealth.details.error").isEqualTo("org.springframework.web.client.HttpClientErrorException: 418 418");
        assertThatJson(response.getBody()).node("status").isEqualTo("DOWN");
        assertThat(response.getStatusCodeValue()).isEqualTo(503);
    }
}