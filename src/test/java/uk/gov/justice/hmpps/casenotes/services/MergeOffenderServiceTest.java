package uk.gov.justice.hmpps.casenotes.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.hmpps.casenotes.dto.BookingIdentifier;
import uk.gov.justice.hmpps.casenotes.dto.OffenderBooking;
import uk.gov.justice.hmpps.casenotes.dto.OffenderEvent;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)

public class MergeOffenderServiceTest {

    private static final String OFFENDER_NO = "A1234AA";
    private static final String MERGED_OFFENDER_NO = "B1234BB";
    public static final long BOOKING_ID = -1L;

    @Mock
    private ExternalApiService externalApiService;

    @Mock
    private OffenderCaseNoteRepository repository;

    private MergeOffenderService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new MergeOffenderService(externalApiService, repository);
    }

    @Test
    public void testCheckForExistingCaseNotesThatNeedMerging() {
        when(externalApiService.getIdentifiersByBookingId(eq(BOOKING_ID)))
                .thenReturn(List.of(
                        BookingIdentifier.builder().type("MERGED").value(MERGED_OFFENDER_NO).build(),
                        BookingIdentifier.builder().type("PNC").value("XX/11XX").build()
                ));

        when(externalApiService.getBooking(eq(BOOKING_ID)))
                .thenReturn(Optional.of(OffenderBooking.builder()
                        .bookingId(BOOKING_ID)
                        .offenderNo(OFFENDER_NO)
                        .build()));

        final var numRows = 5;
        when(repository.updateOffenderIdentifier(eq(MERGED_OFFENDER_NO), eq(OFFENDER_NO)))
                .thenReturn(numRows);

        final var rowsUpdated = service.checkAndMerge(OffenderEvent.builder().bookingId(BOOKING_ID).build());

        assertThat(rowsUpdated).isEqualTo(numRows);
        verify(externalApiService).getIdentifiersByBookingId(eq(BOOKING_ID));
        verify(externalApiService).getBooking(eq(BOOKING_ID));
        verify(repository).updateOffenderIdentifier(eq(MERGED_OFFENDER_NO), eq(OFFENDER_NO));

    }

}
