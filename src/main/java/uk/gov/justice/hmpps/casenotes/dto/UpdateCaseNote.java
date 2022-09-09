package uk.gov.justice.hmpps.casenotes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Hidden;;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;


@Schema(description = "Amend a Case Note")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Data
public class UpdateCaseNote {
    @Schema(required = true, description = "Text of case note", example = "This is a case note message")
    @NotBlank
    @Length(max = 30000)
    private String text;
}
