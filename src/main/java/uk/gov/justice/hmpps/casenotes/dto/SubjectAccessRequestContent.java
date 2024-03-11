package uk.gov.justice.hmpps.casenotes.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import java.time.LocalDateTime;
import java.util.List;


@Schema(description = "SubjectAccessResponse")
@Data
@Jacksonized
@Builder
public class SubjectAccessRequestContent {

    private String type;
    private String subType;
    private LocalDateTime creationDateTime;
    private String authorName;
    private String text;
    private List<CaseNoteAmendment> amendments;

}
