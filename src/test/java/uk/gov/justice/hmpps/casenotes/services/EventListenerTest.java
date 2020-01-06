package uk.gov.justice.hmpps.casenotes.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventListenerTest {

    @Mock
    private CaseNoteService caseNoteService;

    @Mock
    private MergeOffenderService mergeOffenderService;

    private EventListener eventListener;

    @Before
    public void setup() {
        eventListener = new EventListener(caseNoteService, mergeOffenderService);
    }

    @Test
    public void testDeleteEvent() {

        when(caseNoteService.deleteCaseNotesForOffender(eq("A1234AA"))).thenReturn(3);

        eventListener.handleEvents("{\n" +
                "  \"MessageId\": \"message1\",\n" +
                "  \"Type\": \"Notification\",\n" +
                "  \"Timestamp\": \"2019-11-11T11:11:11.111111Z\",\n" +
                "  \"Message\": \"{\\\"offenderIdDisplay\\\":\\\"A1234AA\\\"}\",\n" +
                "  \"TopicArn\": \"arn:aws:sns:eu-west-2:000000000000:offender_events\",\n" +
                "  \"MessageAttributes\": {\n" +
                "    \"eventType\": {\n" +
                "      \"Type\": \"String\",\n" +
                "      \"Value\": \"DATA_COMPLIANCE_DELETE-OFFENDER\"\n" +
                "    },\n" +
                "    \"contentType\": {\n" +
                "      \"Type\": \"String\",\n" +
                "      \"Value\": \"text/plain;charset=UTF-8\"\n" +
                "    }\n" +
                "  }\n" +
                "}");

        Mockito.verify(caseNoteService).deleteCaseNotesForOffender(eq("A1234AA"));
    }


    @Test
    public void testMergeEvent() {

        when(mergeOffenderService.checkAndMerge(eq(100001L))).thenReturn(2);

        eventListener.handleEvents("{\n" +
                "  \"MessageId\": \"message2\",\n" +
                "  \"Type\": \"Notification\",\n" +
                "  \"Timestamp\": \"2019-11-11T11:11:11.111111Z\",\n" +
                "  \"Message\": \"{\\\"eventType\\\": \\\"BOOKING_NUMBER-CHANGED\\\", \\\"bookingId\\\": 100001}\",\n" +
                "  \"TopicArn\": \"arn:aws:sns:eu-west-2:000000000000:offender_events\",\n" +
                "  \"MessageAttributes\": {\n" +
                "    \"eventType\": {\n" +
                "      \"Type\": \"String\",\n" +
                "      \"Value\": \"BOOKING_NUMBER-CHANGED\"\n" +
                "    },\n" +
                "    \"contentType\": {\n" +
                "      \"Type\": \"String\",\n" +
                "      \"Value\": \"text/plain;charset=UTF-8\"\n" +
                "    }\n" +
                "  }\n" +
                "}");

        Mockito.verify(mergeOffenderService).checkAndMerge(eq(100001L));
    }

}
