package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
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
public class CaseNoteType implements Comparable<CaseNoteType>{

    @ApiModelProperty(required = true, value = "Case Note Code", position = 1, example = "OBSERVE")
    private String code;

    @ApiModelProperty(required = true, value = "Case Note description.", position = 2, example = "Observations")
    private String description;

    @ApiModelProperty(required = true, value = "Active indicator flag.", example = "Y", allowableValues = "Y,N", position = 3)
    @Builder.Default
    private String activeFlag = "Y";

    @ApiModelProperty(value = "Source of Case Note Type, legacy case note are null", example = "OCNS", position = 4)
    private String source;

    @ApiModelProperty(value = "Indicates the type of note is sensitive", example = "true", position = 5)
    private boolean sensitive;

    @ApiModelProperty(value = "List of case note sub types", position = 6, allowEmptyValue = true)
    @Builder.Default
    private List<CaseNoteType> subCodes = new ArrayList<>();

    @Override
    public int compareTo(final CaseNoteType type) {
        return getDescription().compareToIgnoreCase(type.getDescription());
    }
}
