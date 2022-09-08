package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Hidden;;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Schema(description = "Create a New Case Note Type")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class NewCaseNoteType {

    @Schema(required = true, description = "Type of case note", example = "GEN")
    @Length(max = 12)
    @NotBlank
    private String type;

    @Schema(required = true, description = "Type Description", example = "General Note Type")
    @Length(max = 80)
    @NotBlank
    private String description;

    @Schema(description = "Active Type, default true", example = "true")
    @Builder.Default
    private boolean active = true;

    @Schema(description = "Sensitive Case Note Type, default true", example = "true")
    @Builder.Default
    private boolean sensitive = true;

    @Schema(description = "Restricted Use, default true", example = "true")
    @Builder.Default
    private boolean restrictedUse = true;
}
