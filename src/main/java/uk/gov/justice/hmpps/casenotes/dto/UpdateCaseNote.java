package uk.gov.justice.hmpps.casenotes.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;


@ApiModel(description = "Amend a Case Note")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Data
public class UpdateCaseNote {
    @ApiModelProperty(required = true, value = "Text of case note", example = "This is a case note message", position = 5)
    @Length(max = 4000)
    @NotBlank
    private String text;
}
