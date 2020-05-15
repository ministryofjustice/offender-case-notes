package uk.gov.justice.hmpps.casenotes.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import uk.gov.justice.hmpps.casenotes.dto.BookingIdentifier;
import uk.gov.justice.hmpps.casenotes.dto.OffenderBooking;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MergeOffenderServiceTest {

    private static final String OFFENDER_NO = "A1234AA";
    private static final String MERGED_OFFENDER_NO = "B1234BB";
    public static final long BOOKING_ID = -1L;

    @Mock
    private ExternalApiService externalApiService;

    @Mock
    private OffenderCaseNoteRepository repository;

    private MergeOffenderService service;

    @BeforeEach
    public void setUp() {
        service = new MergeOffenderService(externalApiService, repository);
    }

    @Test
    public void testCheckForExistingCaseNotesThatNeedMerging() {
        when(externalApiService.getMergedIdentifiersByBookingId(BOOKING_ID))
                .thenReturn(List.of(
                        BookingIdentifier.builder().type("MERGED").value(MERGED_OFFENDER_NO).build()
                ));

        when(externalApiService.getBooking(BOOKING_ID))
                .thenReturn(OffenderBooking.builder()
                        .bookingId(BOOKING_ID)
                        .offenderNo(OFFENDER_NO)
                        .build());

        final var numRows = 5;
        when(repository.updateOffenderIdentifier(MERGED_OFFENDER_NO, OFFENDER_NO))
                .thenReturn(numRows);

        final var rowsUpdated = service.checkAndMerge(BOOKING_ID);

        assertThat(rowsUpdated).isEqualTo(numRows);
        verify(externalApiService).getMergedIdentifiersByBookingId(BOOKING_ID);
        verify(externalApiService).getBooking(BOOKING_ID);
        verify(repository).updateOffenderIdentifier(MERGED_OFFENDER_NO, OFFENDER_NO);
    }

    @Test
    public void testCountingOfMultipleRows() {
        when(externalApiService.getMergedIdentifiersByBookingId(BOOKING_ID))
                .thenReturn(List.of(
                        BookingIdentifier.builder().type("MERGED").value(MERGED_OFFENDER_NO).build(),
                        BookingIdentifier.builder().type("MERGED").value("C1234CC").build()
                ));

        when(externalApiService.getBooking(BOOKING_ID))
                .thenReturn(OffenderBooking.builder()
                        .bookingId(BOOKING_ID)
                        .offenderNo(OFFENDER_NO)
                        .build());

        when(repository.updateOffenderIdentifier(MERGED_OFFENDER_NO, OFFENDER_NO)).thenReturn(2);
        when(repository.updateOffenderIdentifier("C1234CC", OFFENDER_NO)).thenReturn(3);

        final var rowsUpdated = service.checkAndMerge(BOOKING_ID);

        assertThat(rowsUpdated).isEqualTo(5);
        verify(externalApiService).getMergedIdentifiersByBookingId(BOOKING_ID);
        verify(externalApiService).getBooking(BOOKING_ID);
        verify(repository).updateOffenderIdentifier(MERGED_OFFENDER_NO, OFFENDER_NO);
        verify(repository).updateOffenderIdentifier("C1234CC", OFFENDER_NO);
    }

    @Test
    public void testCheckForExistingCaseNotesThatNeedMergingNoMergeFound() {
        when(externalApiService.getMergedIdentifiersByBookingId(BOOKING_ID))
                .thenReturn(List.of());

        final var rowsUpdated = service.checkAndMerge(BOOKING_ID);

        assertThat(rowsUpdated).isEqualTo(0);
        verify(externalApiService).getMergedIdentifiersByBookingId(BOOKING_ID);
    }

    @Test
    public void testCheckForExistingCaseNotesThatNeedMergingNoBookingFound() {
        when(externalApiService.getBooking(BOOKING_ID))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(), null, null));

        assertThatThrownBy(() -> service.checkAndMerge(BOOKING_ID))
                .isInstanceOf(RestClientException.class);
    }

}
