package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Case Note Type")
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonPropertyOrder({"code", "description", "activeFlag", "subCodes"})
@Data
@ToString
@Builder(toBuilder = true)
public class CaseNoteTypeDto implements Comparable<CaseNoteTypeDto>{

    @Schema(required = true, description = "Case Note Code", example = "OBSERVE")
    private String code;

    @Schema(required = true, description = "Case Note description.", example = "Observations")
    private String description;

    @Schema(required = true, description = "Active indicator flag.", example = "Y", allowableValues = "Y,N")
    @Builder.Default
    private String activeFlag = "Y";

    @Schema(description = "Source of Case Note Type, legacy case note are null", example = "OCNS")
    private String source;

    @Schema(description = "Indicates the type of note is sensitive", example = "true")
    private boolean sensitive;

    @Schema(description = "Indicates the type of note can only be created by a sub-set of users (e.g. POMs)", example = "true")
    private boolean restrictedUse;

    @Schema(description = "List of case note sub types", nullable = true)
    @Builder.Default
    private List<CaseNoteTypeDto> subCodes = new ArrayList<>();

    @Override
    public int compareTo(final CaseNoteTypeDto type) {
        return getDescription().compareToIgnoreCase(type.getDescription());
    }
}
