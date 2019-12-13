package uk.gov.justice.hmpps.casenotes.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.casenotes.dto.OffenderEvent;

import java.io.IOException;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "sqs.provider")
@Slf4j
@AllArgsConstructor
public class EventListener {

    private final ObjectMapper objectMapper;
    private final MergeOffenderService mergeOffenderService;

    @JmsListener(destination = "${sqs.queue.name}")
    public void eventListener(final String requestJson) {
        final var event = getOffenderEvent(requestJson);
        if (event != null) {
            mergeOffenderService.checkForMerge(event);
        }
    }

    private OffenderEvent getOffenderEvent(final String requestJson) {
        OffenderEvent event = null;
        try {
            final Map<String, String> message = objectMapper.readValue(requestJson, Map.class);
            if (message != null && message.get("Message") != null) {
                event = objectMapper.readValue(message.get("Message"), OffenderEvent.class);
            }
        } catch (IOException e) {
            log.error("Failed to Parse Message {}", requestJson);
        }
        return event;
    }


}
