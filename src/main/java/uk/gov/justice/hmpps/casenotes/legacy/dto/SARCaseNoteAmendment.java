package uk.gov.justice.hmpps.casenotes.legacy.dto;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * Subject Access Request Case Note Amendment
 **/
@SuppressWarnings("unused")
@Schema(description = "SubjectAccessResponse")
@Data
@Jacksonized
@Builder
public class SARCaseNoteAmendment {

    private LocalDateTime creationDateTime;
    private String authorName;
    private String additionalNoteText;

}
