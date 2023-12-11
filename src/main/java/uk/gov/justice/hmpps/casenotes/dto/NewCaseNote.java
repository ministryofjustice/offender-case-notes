package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

@Schema(description = "Create a Case Note")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class NewCaseNote {

    @Schema(required = true, description = "Location where case note was made, if blank it will be looked up in Nomis", example = "MDI")
    @Length(max = 6)
    private String locationId;

    @Schema(required = true, description = "Type of case note", example = "GEN")
    @Length(max = 12)
    @NotBlank
    private String type;

    @Schema(required = true, description = "Sub Type of case note", example = "OBS")
    @Length(max = 12)
    @NotBlank
    private String subType;

    @Schema(required = true, description = "Occurrence time of case note", example = "2019-01-17T10:25:00")
    private LocalDateTime occurrenceDateTime;

    @Schema(required = true, description = "Text of case note", example = "This is a case note message")
    @Length(max = 30000)
    @NotBlank
    private String text;

}
