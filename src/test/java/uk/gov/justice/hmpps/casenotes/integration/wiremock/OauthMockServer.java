package uk.gov.justice.hmpps.casenotes.integration.wiremock;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class OauthMockServer extends WireMockRule {
    private final Gson gson = new GsonBuilder().create();

    private static final int WIREMOCK_PORT = 8998;

    private static String API_PREFIX = "/auth/api";

    public OauthMockServer() {
        super(WIREMOCK_PORT);
    }

    public StubMapping subGetUserDetails() {
        return stubFor(
                WireMock.get(WireMock.urlPathMatching(API_PREFIX + "/user/\\w+"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"staffId\": 1111,\n" +
                                        "  \"username\": \"ITAG_USER\",\n" +
                                        "  \"active\": true,\n" +
                                        "  \"name\": \"Mikey Mouse\",\n" +
                                        "  \"authSource\": \"nomis\",\n" +
                                        "  \"activeCaseLoadId\": \"LEI\"\n" +
                                        "}")
                                .withStatus(200)
                        ));

    }


}
