package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@ApiModel(description = "Case Note Count")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class CaseNoteCount {

    @ApiModelProperty(required = true, value = "Offender Unique Identifier", position = 1, example = "A1234AA")
    @NotNull
    private Long bookingId;

    @ApiModelProperty(required = true, value = "Case Note Type", position = 2, example = "KA")
    @NotBlank
    private String type;

    @ApiModelProperty(required = true, value = "Case Note Sub Type", position = 3, example = "KS")
    @NotBlank
    private String subType;

    @ApiModelProperty(required = true, value = "Count", position = 4, example = "1")
    @NotBlank
    private Long count;

    @ApiModelProperty(required = true, value = "From Date", position = 5, example = "2017-10-31")
    @NotNull
    private LocalDate fromDate;

    @ApiModelProperty(required = true, value = "To Date", position = 6, example = "2017-10-31")
    @NotNull
    private LocalDate toDate;
}

