package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Hidden;;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Case Note Amendment
 **/
@SuppressWarnings("unused")
@Schema(description = "Case Note Amendment")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class CaseNoteAmendment {
    @Schema(required = true, description = "Amendment Case Note Id (unique)", example = "123232")
    @NotNull
    private Long caseNoteAmendmentId;

    @Schema(required = true, description = "Date and Time of Case Note creation", example = "2018-12-01T13:45:00")
    @NotNull
    private LocalDateTime creationDateTime;

    @Schema(required = true, description = "Username of the user amending the case note", example = "USER1")
    @NotBlank
    private String authorUserName;

    @Schema(required = true, description = "Name of the user amending the case note", example = "Mickey Mouse")
    @NotBlank
    private String authorName;

    @Schema(required = true, description = "User Id of the user amending the case note - staffId for nomis users, userId for auth users", example = "12345")
    @NotNull
    private String authorUserId;

    @Schema(required = true, description = "Additional Case Note Information", example = "Some Additional Text")
    @NotBlank
    private String additionalNoteText;

}
