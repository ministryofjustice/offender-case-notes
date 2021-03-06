package uk.gov.justice.hmpps.casenotes.services;

import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wiremock.org.apache.commons.io.IOUtils;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventListenerTest {

    @Mock
    private CaseNoteService caseNoteService;

    @Mock
    private MergeOffenderService mergeOffenderService;

    private EventListener eventListener;

    @BeforeEach
    public void setup() {
        eventListener = new EventListener(caseNoteService, mergeOffenderService, new GsonBuilder().create());
    }

    @Test
    public void testDeleteEvent() throws IOException {
        when(caseNoteService.deleteCaseNotesForOffender(eq("A1234AA"))).thenReturn(3);

        eventListener.handleEvents(getJson("offender-deletion-request.json"));

        verify(caseNoteService).deleteCaseNotesForOffender(eq("A1234AA"));
    }


    @Test
    public void testMergeEvent() throws IOException {
        when(mergeOffenderService.checkAndMerge(eq(100001L))).thenReturn(2);

        eventListener.handleEvents(getJson("booking-number-changed.json"));

        verify(mergeOffenderService).checkAndMerge(eq(100001L));
    }

    private String getJson(final String filename) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(filename), UTF_8.toString());
    }
}
