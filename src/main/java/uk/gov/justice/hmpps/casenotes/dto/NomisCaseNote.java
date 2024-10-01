package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class NomisCaseNote {

    @JsonProperty("caseNoteId")
    private Integer id;
    @JsonProperty("offenderIdentifier")
    private String personIdentifier;
    private String type;
    private String typeDescription;
    private String subType;
    private String subTypeDescription;
    private String source;
    @JsonProperty("creationDateTime")
    private LocalDateTime createdAt;
    @JsonProperty("occurrenceDateTime")
    private LocalDateTime occurredAt;
    private Long staffId;
    private String authorUsername;
    private String authorName;
    private String text;
    private String originalNoteText;
    private String agencyId;

    @Builder.Default
    private List<NomisCaseNoteAmendment> amendments = new ArrayList<>();
}
