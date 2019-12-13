package uk.gov.justice.hmpps.casenotes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OffenderBooking {
    private Long bookingId;
    private String offenderNo;
}