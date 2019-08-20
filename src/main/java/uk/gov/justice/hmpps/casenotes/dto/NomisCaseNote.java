package uk.gov.justice.hmpps.casenotes.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class NomisCaseNote {

    private Long caseNoteId;
    private String offenderIdentifier;
    private String type;
    private String typeDescription;
    private String subType;
    private String subTypeDescription;
    private String source;
    private LocalDateTime creationDateTime;
    private LocalDateTime occurrenceDateTime;
    private Long staffId;
    private String authorName;
    private String text;
    private String originalNoteText;
    private String agencyId;

    @Builder.Default
    private List<CaseNoteAmendment> amendments = new ArrayList<>();
}
