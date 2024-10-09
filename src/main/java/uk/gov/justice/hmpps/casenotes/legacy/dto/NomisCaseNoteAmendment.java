package uk.gov.justice.hmpps.casenotes.legacy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * NOMIS Case Note Amendment
 **/
@SuppressWarnings("unused")
@Schema(description = "NOMIS Case Note Amendment")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class NomisCaseNoteAmendment {
    @Schema(requiredMode = REQUIRED, description = "Amendment Case Note Id (unique)", example = "123232")
    @NotNull
    private Long caseNoteAmendmentId;

    @Schema(requiredMode = REQUIRED, description = "Date and Time of Case Note creation", example = "2018-12-01T13:45:00")
    @NotNull
    private LocalDateTime creationDateTime;

    @Schema(requiredMode = REQUIRED, description = "Username of the user amending the case note", example = "AAA11B")
    @NotBlank
    private String authorUsername;

    @Schema(requiredMode = REQUIRED, description = "Name of the user amending the case note", example = "Mickey Mouse")
    @NotBlank
    private String authorName;

    @Schema(requiredMode = REQUIRED, description = "User Id of the user amending the case note - staffId for nomis users, userId for auth users", example = "12345")
    @NotNull
    private String authorUserId;

    @Schema(requiredMode = REQUIRED, description = "Additional Case Note Information", example = "Some Additional Text")
    @NotBlank
    private String additionalNoteText;

}
