package uk.gov.justice.hmpps.casenotes.integration.wiremock;


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.*;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteEvent;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteType;
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class Elite2MockServer extends WireMockRule {
    private final Gson gson;

    private static final int WIREMOCK_PORT = 8999;

    private static final String API_PREFIX = "/api";

    public Elite2MockServer() {
        super(WIREMOCK_PORT);

        gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter()).create();
    }

    public void subGetCaseNoteTypes() {
        final var getCaseNoteTypes = API_PREFIX + "/reference-domains/caseNoteTypes";
        stubFor(
                get(urlPathEqualTo(getCaseNoteTypes))
                        .willReturn(aResponse()
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
                get(urlPathEqualTo(getCaseNoteTypes))
                        .willReturn(aResponse()
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
        final var getCaseNoteTypes = API_PREFIX + "/bookings/offenderNo/" + offenderIdentifier;
        stubFor(
                get(urlPathMatching(getCaseNoteTypes))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"bookingId\": 1,\n" +
                                        "  \"offenderNo\": \"" + offenderIdentifier + "\",\n" +
                                        "  \"agencyId\": \"LEI\"" +
                                        "}")
                                .withStatus(200)
                        ));

    }

    public void subGetCaseNotesForOffender(final String offenderIdentifier) {
        final var getCaseNotes = API_PREFIX + "/offenders/" + offenderIdentifier + "/case-notes";
        final var body = gson.toJson(List.of(createNomisCaseNote()));
        stubFor(
                get(urlPathMatching(getCaseNotes))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withHeader("Total-Records", "1")
                                .withHeader("Page-Offset", "0")
                                .withHeader("Page-Limit", "10")
                                .withBody(body)
                                .withStatus(200)
                        ));

    }

    public void subGetCaseNoteForOffender(final String offenderIdentifier, final Long caseNoteIdentifier) {
        final var getCaseNote = String.format("%s/offenders/%s/case-notes/%s", API_PREFIX, offenderIdentifier, caseNoteIdentifier);
        final var body = gson.toJson(createNomisCaseNote());
        stubFor(
                get(urlPathMatching(getCaseNote))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(body)
                                .withStatus(200)
                        ));
    }

    private NomisCaseNote createNomisCaseNote() {
        return NomisCaseNote.builder()
                .caseNoteId(131232L)
                .agencyId("LEI")
                .authorName("Mickey Mouse")
                .creationDateTime(LocalDateTime.now().minusMonths(1))
                .source("INST")
                .originalNoteText("Some Text")
                .staffId(1231232L)
                .type("OBS")
                .subType("GEN")
                .typeDescription("Observation")
                .subTypeDescription("General")
                .text("Some Text")
                .occurrenceDateTime(LocalDateTime.now().minusMonths(1))
                .build();
    }

    public void subCreateCaseNote(final String offenderIdentifier) {
        final var body = gson.toJson(createNomisCaseNote());
        stubFor(post(urlPathMatching(String.format("%s/offenders/%s/case-notes", API_PREFIX, offenderIdentifier)))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                        .withStatus(201)
                ));

    }

    public void subAmendCaseNote(final String offenderIdentifier, final String caseNoteIdentifier) {
        final var body = gson.toJson(createNomisCaseNote());
        stubFor(put(urlPathMatching(String.format("%s/offenders/%s/case-notes/%s", API_PREFIX, offenderIdentifier, caseNoteIdentifier)))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                        .withStatus(200)
                ));
    }

    public void subPing(final int status) {
        stubFor(get("/ping").willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(status == 200 ? "pong" : "some error")
                .withStatus(status)));
    }

    public void stubGetCaseNoteEvents() {
        final var body = gson.toJson(createEvents());
        stubFor(get(urlPathMatching("/api/case-notes/events?.*")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)
                .withStatus(200)
        ));
    }

    private List<CaseNoteEvent> createEvents() {
        return List.of(
                createCaseNoteEvent(1, "a note", LocalDateTime.parse("2019-02-03T11:10:09")),
                createCaseNoteEvent(2, "another note", LocalDateTime.parse("2018-01-02T11:10:09")));
    }

    private CaseNoteEvent createCaseNoteEvent(final int id, final String noteText, final LocalDateTime modifyDateTime) {
        return CaseNoteEvent.builder()
                .content(noteText)
                .notificationTimestamp(modifyDateTime)
                .nomsId("AB" + id)
                .id(id)
                .noteType("MAIN SUB")
                .staffName("Staff name")
                .build();
    }

    public void stubGetCaseNoteEventsNoLimit() {
        final var body = gson.toJson(createEvents());
        stubFor(get(urlPathMatching("/api/case-notes/events_no_limit?.*")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)
                .withStatus(200)
        ));
    }

    private static class LocalDateTimeConverter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public LocalDateTime deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsJsonPrimitive().getAsString());
        }

        @Override
        public JsonElement serialize(final LocalDateTime src, final Type typeOfSrc, final JsonSerializationContext context) {
            return new JsonPrimitive(FORMATTER.format(src));
        }
    }

}
