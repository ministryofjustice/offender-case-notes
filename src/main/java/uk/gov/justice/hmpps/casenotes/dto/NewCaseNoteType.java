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

@ApiModel(description = "Create a New Case Note Type")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class NewCaseNoteType {

    @ApiModelProperty(required = true, value = "Type of case note", example = "GEN", position = 1)
    @Length(max = 12)
    @NotBlank
    private String type;

    @ApiModelProperty(required = true, value = "Type Description", example = "General Note Type", position = 2)
    @Length(max = 80)
    @NotBlank
    private String description;

    @ApiModelProperty(value = "Active Type, default true", example = "true", position = 3)
    @Builder.Default
    private boolean active = true;

    @ApiModelProperty(value = "Sensitive Case Note Type, default true", example = "true", position = 4)
    @Builder.Default
    private boolean sensitive = true;

    @ApiModelProperty(value = "Restricted Use, default true", example = "true", position = 5)
    @Builder.Default
    private boolean restrictedUse = true;
}
