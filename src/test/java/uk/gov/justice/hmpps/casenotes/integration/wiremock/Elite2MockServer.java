package uk.gov.justice.hmpps.casenotes.integration.wiremock;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteType;

import java.util.List;

public class Elite2MockServer extends WireMockRule {
    private final Gson gson = new GsonBuilder().create();

    private static final int WIREMOCK_PORT = 8999;

    private static final String API_PREFIX = "/api";

    public Elite2MockServer() {
        super(WIREMOCK_PORT);
    }

    public void subGetCaseNoteTypes() {
        final var getCaseNoteTypes = API_PREFIX + "/reference-domains/caseNoteTypes";
        stubFor(
                WireMock.get(WireMock.urlPathEqualTo(getCaseNoteTypes))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(gson.toJson(List.of(
                                        CaseNoteType.builder().code("KA").description("Key worker")
                                                .subCodes(List.of(
                                                        CaseNoteType.builder().code("KS").description("Key worker Session").build(),
                                                        CaseNoteType.builder().code("KE").description("Key worker Entry").build()
                                                )),
                                        CaseNoteType.builder().code("OBS").description("Observation")
                                                .subCodes(List.of(
                                                        CaseNoteType.builder().code("GEN").description("General").build(),
                                                        CaseNoteType.builder().code("SPECIAL").description("Special").build()
                                                        )
                                                ))))
                                .withStatus(200)
                        ));

    }

    public void subUserCaseNoteTypes() {
        final var getCaseNoteTypes = API_PREFIX + "/users/me/caseNoteTypes";
        stubFor(
                WireMock.get(WireMock.urlPathEqualTo(getCaseNoteTypes))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(gson.toJson(List.of(
                                        CaseNoteType.builder().code("KA").description("Key worker")
                                                .subCodes(List.of(
                                                        CaseNoteType.builder().code("KS").description("Key worker Session").build()
                                                )),
                                        CaseNoteType.builder().code("OBS").description("Observation")
                                                .subCodes(List.of(
                                                        CaseNoteType.builder().code("GEN").description("General").build()
                                                        )
                                                ))))
                                .withStatus(200)
                        ));

    }

    public void subGetOffender(final String offenderIdentifier) {
        final var getCaseNoteTypes = API_PREFIX + "/bookings/offenderNo/"+offenderIdentifier;
        stubFor(
                WireMock.get(WireMock.urlPathMatching(getCaseNoteTypes))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"bookingId\": 1,\n" +
                                        "  \"offenderNo\": \"" + offenderIdentifier + "\",\n" +
                                        "  \"agencyId\": \"LEI\"" +
                                        "}")
                                .withStatus(200)
                        ));

    }

}
