package uk.gov.justice.hmpps.casenotes.services

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import uk.gov.justice.hmpps.casenotes.dto.BookingIdentifier
import uk.gov.justice.hmpps.casenotes.dto.OffenderBooking
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository

@ExtendWith(MockitoExtension::class)
class MergeOffenderServiceTest {

  private val externalApiService: ExternalApiService = mock()

  private val repository: OffenderCaseNoteRepository = mock()

  private lateinit var service: MergeOffenderService

  @BeforeEach
  fun setUp() {
    service = MergeOffenderService(externalApiService, repository)
  }

  @Test
  fun testCheckForExistingCaseNotesThatNeedMerging() {
    whenever(externalApiService.getMergedIdentifiersByBookingId(BOOKING_ID))
        .thenReturn(listOf(
            BookingIdentifier("MERGED", MERGED_OFFENDER_NO)
        ))
    whenever(externalApiService.getBooking(BOOKING_ID))
        .thenReturn(OffenderBooking.builder()
            .bookingId(BOOKING_ID)
            .offenderNo(OFFENDER_NO)
            .build())
    val numRows = 5
    whenever(repository.updateOffenderIdentifier(MERGED_OFFENDER_NO, OFFENDER_NO))
        .thenReturn(numRows)
    val rowsUpdated = service.checkAndMerge(BOOKING_ID)
    Assertions.assertThat(rowsUpdated).isEqualTo(numRows)
    verify(externalApiService).getMergedIdentifiersByBookingId(BOOKING_ID)
    verify(externalApiService).getBooking(BOOKING_ID)
    verify(repository).updateOffenderIdentifier(MERGED_OFFENDER_NO, OFFENDER_NO)
  }

  @Test
  fun testCountingOfMultipleRows() {
    whenever(externalApiService.getMergedIdentifiersByBookingId(BOOKING_ID))
        .thenReturn(listOf(
            BookingIdentifier("MERGED", MERGED_OFFENDER_NO),
            BookingIdentifier("MERGED", "C1234CC")
        ))
    whenever(externalApiService.getBooking(BOOKING_ID))
        .thenReturn(OffenderBooking.builder()
            .bookingId(BOOKING_ID)
            .offenderNo(OFFENDER_NO)
            .build())
    whenever(repository.updateOffenderIdentifier(MERGED_OFFENDER_NO, OFFENDER_NO)).thenReturn(2)
    whenever(repository.updateOffenderIdentifier("C1234CC", OFFENDER_NO)).thenReturn(3)
    val rowsUpdated = service.checkAndMerge(BOOKING_ID)
    Assertions.assertThat(rowsUpdated).isEqualTo(5)
    verify(externalApiService).getMergedIdentifiersByBookingId(BOOKING_ID)
    verify(externalApiService).getBooking(BOOKING_ID)
    verify(repository).updateOffenderIdentifier(MERGED_OFFENDER_NO, OFFENDER_NO)
    verify(repository).updateOffenderIdentifier("C1234CC", OFFENDER_NO)
  }

  @Test
  fun testCheckForExistingCaseNotesThatNeedMergingNoMergeFound() {
    whenever(externalApiService.getMergedIdentifiersByBookingId(BOOKING_ID))
        .thenReturn(listOf())
    val rowsUpdated = service.checkAndMerge(BOOKING_ID)
    Assertions.assertThat(rowsUpdated).isEqualTo(0)
    verify(externalApiService).getMergedIdentifiersByBookingId(BOOKING_ID)
  }

  @Test
  fun testCheckForExistingCaseNotesThatNeedMergingNoBookingFound() {
    whenever(externalApiService.getBooking(BOOKING_ID))
        .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders(), null, null))
    Assertions.assertThatThrownBy { service.checkAndMerge(BOOKING_ID) }
        .isInstanceOf(RestClientException::class.java)
  }

  companion object {
    private const val OFFENDER_NO = "A1234AA"
    private const val MERGED_OFFENDER_NO = "B1234BB"
    const val BOOKING_ID = -1L
  }
}
