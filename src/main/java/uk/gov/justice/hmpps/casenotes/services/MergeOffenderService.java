package uk.gov.justice.hmpps.casenotes.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.casenotes.dto.OffenderEvent;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;

import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
@Service
@Slf4j
@Transactional
public class MergeOffenderService {

    private final ExternalApiService externalApiService;
    private final OffenderCaseNoteRepository repository;

    public int checkAndMerge(final OffenderEvent offenderEvent) {
        final var rowsUpdated = new AtomicInteger();
        log.debug("Check for merged booking for ID {}", offenderEvent.getBookingId());
        externalApiService.getIdentifiersByBookingId(offenderEvent.getBookingId()).stream()
                .filter(id -> "MERGED".equals(id.getType()))
                .forEach(id -> {
                    final var booking = externalApiService.getBooking(offenderEvent.getBookingId());
                    rowsUpdated.set(repository.updateOffenderIdentifier(id.getValue(), booking.getOffenderNo()));
                    if (rowsUpdated.get() > 0) {
                        log.info("{} case notes were merged from offender identifier {} to {}", rowsUpdated, id.getValue(), booking.getOffenderNo());
                    }
                });

        if (rowsUpdated.get() == 0) {
            log.debug("No records to merge for booking ID {}", offenderEvent.getBookingId());
        }
        return rowsUpdated.get();
    }

}
