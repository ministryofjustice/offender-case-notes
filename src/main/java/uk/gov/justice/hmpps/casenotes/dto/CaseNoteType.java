package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

@ApiModel(description = "Case Note Type")
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonPropertyOrder({"code", "description", "activeFlag", "subCodes"})
@Data
@ToString
@Builder(toBuilder = true)
public class CaseNoteType {

    @ApiModelProperty(required = true, value = "Case Note Code", position = 1, example = "OBSERVE")
    private String code;

    @ApiModelProperty(required = true, value = "Case Note description.", position = 2, example = "Observations")
    private String description;

    @ApiModelProperty(required = true, value = "Active indicator flag.", example = "Y", allowableValues = "Y,N", position = 3)
    private String activeFlag;

    @ApiModelProperty(value = "List of case note sub types", position = 4, allowEmptyValue = true)
    private List<CaseNoteType> subCodes;
}
