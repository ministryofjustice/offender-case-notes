package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Case Note Filter")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class CaseNoteFilter {
    @Schema(required = true, description = "Filter by Case Note Type", example = "KA")
    private String type;

    @Schema(required = true, description = "Filter by Case Note Sub Type", example = "KS")
    private String subType;

    @Schema(required = true, description = "Filter case notes from this date", example = "2017-10-31T01:30:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @Schema(required = true, description = "Filter case notes up to this date", example = "2019-05-31T01:30:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    @Schema(required = true, description = "Filter by the location", example = "MDI")
    private String locationId;

    @Schema(required = true, description = "Filter by username", example = "USER1")
    private String authorUsername;

    @Schema(required = true, description = "Filter by a list of case note types and optional case not sub types separated by plus", example = "KA+KE,OBS,POM+GEN")
    private List<String> caseNoteTypeSubTypes;

}
