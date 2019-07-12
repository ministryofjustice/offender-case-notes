package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@ApiModel(description = "Case Note")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class CaseNote {

    @ApiModelProperty(required = true, value = "Case Note Id (unique)", example = "12311312")
    @NotNull
    private Long caseNoteId;

    @ApiModelProperty(required = true, value = "Offender Unique Identifier", example = "A1234AA")
    @NotNull
    private String offenderIdentifier;

    @ApiModelProperty(required = true, value = "Case Note Type", position = 2, example = "KA")
    @NotBlank
    private String type;

    @ApiModelProperty(required = true, value = "Case Note Sub Type", position = 4, example = "KS")
    @NotBlank
    private String subType;

    @ApiModelProperty(required = true, value = "Date and Time of Case Note creation", position = 7, example = "2017-10-31T01:30:00")
    @NotNull
    private LocalDateTime creationDateTime;

    @ApiModelProperty(required = true, value = "Date and Time of when case note contact with offender was made", position = 8, example = "2017-10-31T01:30:00")
    @NotNull
    private LocalDateTime occurrenceDateTime;

    @ApiModelProperty(required = true, value = "Id of staff member who created case note", position = 9, example = "USER1")
    @NotNull
    private String staffUsername;

    @ApiModelProperty(required = true, value = "Case Note Text", position = 11, example = "This is some text")
    @NotBlank
    private String text;

    @ApiModelProperty(value = "Location Code where Case Note was made.", position = 13, example = "MDI")
    private String locationId;

    @ApiModelProperty(required = true, value = "Ordered list of amendments to the case note (oldest first)", position = 14)
    @NotNull
    @Builder.Default
    private List<CaseNoteAmendment> amendments = new ArrayList<>();
}
