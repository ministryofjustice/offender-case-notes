package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@ApiModel(description = "Create a Case Note")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class NewCaseNote {

    @ApiModelProperty(required = true, value = "Location where case note was made, if blank it will be looked up in Nomis", example = "MDI", position = 1)
    @Length(max = 6)
    private String locationId;

    @ApiModelProperty(required = true, value = "Type of case note", example = "GEN", position = 2)
    @Length(max = 12)
    @NotBlank
    private String type;

    @ApiModelProperty(required = true, value = "Sub Type of case note", example = "OBS", position = 3)
    @Length(max = 12)
    @NotBlank
    private String subType;

    @ApiModelProperty(required = true, value = "Occurrence time of case note", example = "2019-01-17T10:25:00", position = 4)
    private LocalDateTime occurrenceDateTime;

    @ApiModelProperty(required = true, value = "Text of case note", example = "This is a case note message", position = 5)
    @Length(max = 30000)
    @NotBlank
    private String text;

}
