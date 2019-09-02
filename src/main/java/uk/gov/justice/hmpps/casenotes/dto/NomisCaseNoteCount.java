package uk.gov.justice.hmpps.casenotes.dto;

import lombok.*;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class NomisCaseNoteCount {

    private Long bookingId;
    private String type;
    private String subType;
    private Long count;
    private String fromDate;
    private String toDate;

}
