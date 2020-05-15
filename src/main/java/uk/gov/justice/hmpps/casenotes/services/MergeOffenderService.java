package uk.gov.justice.hmpps.casenotes.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;

@AllArgsConstructor
@Service
@Slf4j
@Transactional
public class MergeOffenderService {

    private final ExternalApiService externalApiService;
    private final OffenderCaseNoteRepository repository;

    public int checkAndMerge(final Long bookingId) {
        log.debug("Check for merged booking for ID {}", bookingId);
        final var booking = externalApiService.getBooking(bookingId);
        final var totalRows = externalApiService.getMergedIdentifiersByBookingId(bookingId).stream()
                .mapToInt(id -> {
                    final var rowsUpdated = repository.updateOffenderIdentifier(id.getValue(), booking.getOffenderNo());
                    if (rowsUpdated > 0) {
                        log.info("{} case notes were merged from offender identifier {} to {}", rowsUpdated, id.getValue(), booking.getOffenderNo());
                    }
                    return rowsUpdated;
                }).sum();

        if (totalRows == 0) {
            log.debug("No records to merge for booking ID {}", bookingId);
        }
        return totalRows;
    }

}
