package uk.gov.justice.hmpps.casenotes.integration.wiremock;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class OauthMockServer extends WireMockRule {

    private static final int WIREMOCK_PORT = 8998;

    private static final String API_PREFIX = "/auth/api";

    public OauthMockServer() {
        super(WIREMOCK_PORT);
    }

    public void subGetUserDetails(final String username) {
        stubFor(
                get(urlPathMatching(String.format("%s/user/%s", API_PREFIX, username)))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"staffId\": 1111,\n" +
                                        "  \"username\": \"" + username + "\",\n" +
                                        "  \"active\": true,\n" +
                                        "  \"name\": \"Mikey Mouse\",\n" +
                                        "  \"authSource\": \"nomis\",\n" +
                                        "  \"activeCaseLoadId\": \"LEI\"\n" +
                                        "}")
                                .withStatus(200)
                        ));

    }

    public void subPing(final int status) {
        stubFor(get("/auth/ping").willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(status == 200 ? "pong" : "some error")
                .withStatus(status)));
    }

    public void subPingDelay(final int status) {
        stubFor(get("/auth/ping").willReturn(aResponse().withFixedDelay(2000)
                .withHeader("Content-Type", "application/json")
                .withBody(status == 200 ? "pong" : "some error")
                .withStatus(status)));
    }

}
