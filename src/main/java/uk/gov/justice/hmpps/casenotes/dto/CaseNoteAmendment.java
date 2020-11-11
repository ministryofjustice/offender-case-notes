package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(description = "Case Note Amendment")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class CaseNoteAmendment {
    @ApiModelProperty(required = true, value = "Amendment Case Note Id (unique)", example = "123232", position = 1)
    @NotNull
    private Long caseNoteAmendmentId;

    @ApiModelProperty(required = true, value = "Sequence Number", example = "1", position = 2)
    @NotNull
    private Integer sequence;

    @ApiModelProperty(required = true, value = "Date and Time of Case Note creation", example = "2018-12-01T13:45:00", position = 3)
    @NotNull
    private LocalDateTime creationDateTime;

    @ApiModelProperty(required = true, value = "Username of the user amending the case note", position = 4, example = "USER1")
    @NotBlank
    private String authorUserName;

    @ApiModelProperty(required = true, value = "Name of the user amending the case note", position = 5, example = "Mickey Mouse")
    @NotBlank
    private String authorName;

    @ApiModelProperty(required = true, value = "User Id of the user amending the case note - staffId for nomis users, userId for auth users", position = 11, example = "12345")
    @NotNull
    private String authorUserId;

    @ApiModelProperty(required = true, value = "Additional Case Note Information", position = 6, example = "Some Additional Text")
    @NotBlank
    private String additionalNoteText;

}
