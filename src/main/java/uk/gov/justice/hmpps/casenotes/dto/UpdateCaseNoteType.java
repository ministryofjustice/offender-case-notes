package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@ApiModel(description = "Update a Case Note Type")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class UpdateCaseNoteType {

    @ApiModelProperty(required = true, value = "Type Description", example = "General Note Type", position = 1)
    @Length(max = 80)
    @NotBlank
    private String description;

    @ApiModelProperty(value = "Active Type", example = "true", position = 2)
    @Builder.Default
    private boolean active = true;
}
