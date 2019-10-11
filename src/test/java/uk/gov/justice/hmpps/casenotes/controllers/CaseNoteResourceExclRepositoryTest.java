package uk.gov.justice.hmpps.casenotes.controllers;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteEvent;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType;
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CaseNoteResourceExclRepositoryTest extends ResourceTest {
    private static final List<String> EVENT_ROLE = List.of("ROLE_CASE_NOTE_EVENTS");

    @MockBean
    private OffenderCaseNoteRepository caseNoteRepository;

    @Test
    public void getCaseNoteEvents_noLimit() {
        elite2MockServer.stubGetCaseNoteEventsNoLimit();

        final var fromDateAsString = "2019-03-02T11:10:09";
        final var fromDate = LocalDateTime.parse(fromDateAsString);
        final var fredEvent = createOffenderCaseNote(-234, "FRED", "JOE");
        final var bobJoeEvent = createOffenderCaseNote(-456, "BOB", "JOE");
        when(caseNoteRepository.findBySensitiveCaseNoteType_ParentType_TypeInAndModifyDateTimeAfterOrderByModifyDateTime(anySet(), any(), any()))
                .thenReturn(List.of(bobJoeEvent, fredEvent, createOffenderCaseNote(-2, "BOB", "OTHER"), createOffenderCaseNote(-4, "WRONG", "TYPE")));

        final var requestEntity = createHttpEntityWithBearerAuthorisation("ITAG_USER", EVENT_ROLE, Map.of());

        final var responseEntity = testRestTemplate.exchange("/case-notes/events_no_limit?type={type}&type=FRED&createdDate=" + fromDate, HttpMethod.GET, requestEntity, String.class, "BOB+JOE");

        assertThatJsonFileAndStatus(responseEntity, 200, "casenoteevents.json");

        verify(caseNoteRepository).findBySensitiveCaseNoteType_ParentType_TypeInAndModifyDateTimeAfterOrderByModifyDateTime(Set.of("BOB", "FRED"), fromDate, PageRequest.of(0, Integer.MAX_VALUE));

        WireMock.verify(getRequestedFor(urlPathEqualTo("/api/case-notes/events_no_limit"))
                .andMatching(request -> {
                    // query param matching can only match one parameter, so need custom matcher instead
                    assertThat(request.getUrl()).contains("?type=BOB+JOE&type=FRED");
                    return MatchResult.exactMatch();
                })
                .withQueryParam("createdDate", equalTo(fromDateAsString))
        );
    }

    @Test
    public void getCaseNoteEvents() {
        elite2MockServer.stubGetCaseNoteEvents();

        final var fromDateAsString = "2019-03-02T11:10:09";
        final var fromDate = LocalDateTime.parse(fromDateAsString);
        final var fredEvent = createOffenderCaseNote(-234, "FRED", "JOE");
        final var bobJoeEvent = createOffenderCaseNote(-456, "BOB", "JOE");
        when(caseNoteRepository.findBySensitiveCaseNoteType_ParentType_TypeInAndModifyDateTimeAfterOrderByModifyDateTime(anySet(), any(), any()))
                .thenReturn(List.of(bobJoeEvent, fredEvent, createOffenderCaseNote(-2, "BOB", "OTHER"), createOffenderCaseNote(-3, "WRONG", "TYPE")));

        final var requestEntity = createHttpEntityWithBearerAuthorisation("ITAG_USER", EVENT_ROLE, Map.of());

        final var responseEntity = testRestTemplate.exchange("/case-notes/events?limit=10&type=BOB+JOE&type=FRED&createdDate=" + fromDate, HttpMethod.GET, requestEntity, String.class);

        assertThatJsonFileAndStatus(responseEntity, 200, "casenoteevents.json");

        verify(caseNoteRepository).findBySensitiveCaseNoteType_ParentType_TypeInAndModifyDateTimeAfterOrderByModifyDateTime(Set.of("BOB", "FRED"), fromDate, PageRequest.of(0, 10));

        WireMock.verify(getRequestedFor(urlPathEqualTo("/api/case-notes/events"))
                .andMatching(request -> {
                    // query param matching can only match one parameter, so need custom matcher instead
                    assertThat(request.getUrl()).contains("?type=BOB%20JOE&type=FRED");
                    return MatchResult.exactMatch();
                })
                .withQueryParam("createdDate", equalTo(fromDateAsString))
                .withQueryParam("limit", equalTo("10"))
        );
    }

    @Test
    public void getCaseNoteEvents_missingLimit() {
        elite2MockServer.stubGetCaseNoteEvents();

        final var requestEntity = createHttpEntityWithBearerAuthorisation("ITAG_USER", EVENT_ROLE, Map.of());
        final var responseEntity = testRestTemplate.exchange("/case-notes/events?&type=BOB+JOE&type=FRED&createdDate=" + LocalDateTime.now(), HttpMethod.GET, requestEntity, String.class);
        assertThatJsonFileAndStatus(responseEntity, 400, "casenoteevents_validation.json");
    }

    private CaseNoteEvent createEvent(final String type, final String subType) {
        return CaseNoteEvent.builder()
                .noteType(type + " " + subType)
                .content("Some content for " + subType)
                .contactTimestamp(LocalDateTime.parse("2019-02-01T23:22:21"))
                .notificationTimestamp(LocalDateTime.parse("2019-02-01T23:22:21"))
                .establishmentCode("LEI")
                .staffName("Last, First")
                .id(1)
                .nomsId(123 + subType)
                .build();
    }

    private OffenderCaseNote createOffenderCaseNote(final int eventId, final String type, final String subType) {
        return OffenderCaseNote.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .occurrenceDateTime(now())
                .locationId("MDI")
                .authorUsername("USER2")
                .authorUserId("some id")
                .authorName("Mickey Mouse")
                .offenderIdentifier("A12" + type + subType)
                .modifyDateTime(LocalDateTime.parse("2019-02-01T23:22:21"))
                .sensitiveCaseNoteType(SensitiveCaseNoteType.builder().type(subType).parentType(ParentNoteType.builder().type(type).build()).build())
                .noteText("Some ocn content for " + subType)
                .build();
    }
}
