package uk.gov.justice.hmpps.casenotes.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.casenotes.dto.OffenderEvent;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;

import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
@Service
@Slf4j
public class MergeOffenderService {

    private final ExternalApiService externalApiService;
    private final OffenderCaseNoteRepository repository;

    public int checkAndMerge(final OffenderEvent offenderEvent) {
        AtomicInteger rowsUpdated = new AtomicInteger();
        log.debug("Check for merged booking for ID {}", offenderEvent.getBookingId());
        externalApiService.getIdentifiersByBookingId(offenderEvent.getBookingId()).stream()
                .filter(id -> "MERGED".equals(id.getType()))
                .forEach(id -> externalApiService.getBooking(offenderEvent.getBookingId())
                        .ifPresent(booking -> {
                            rowsUpdated.set(repository.updateOffenderIdentifier(id.getValue(), booking.getOffenderNo()));
                            if (rowsUpdated.get() > 0) {
                                log.info("{} case notes where merged from offender identifier {} to {}", rowsUpdated, id.getValue(), booking.getOffenderNo());
                            }
                        }));
        return rowsUpdated.get();
    }

}
