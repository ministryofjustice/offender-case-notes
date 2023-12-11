package uk.gov.justice.hmpps.casenotes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;


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
