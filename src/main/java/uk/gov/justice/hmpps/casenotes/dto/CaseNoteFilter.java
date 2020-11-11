package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@ApiModel(description = "Case Note Filter")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Data
public class CaseNoteFilter {
    @ApiModelProperty(required = true, value = "Filter by Case Note Type", position = 1, example = "KA")
    private String type;

    @ApiModelProperty(required = true, value = "Filter by Case Note Sub Type", position = 2, example = "KS")
    private String subType;

    @ApiModelProperty(required = true, value = "Filter case notes from this date", position = 3, example = "2017-10-31T01:30:00", dataType = "LocalDateTime")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @ApiModelProperty(required = true, value = "Filter case notes up to this date", position = 4, example = "2019-05-31T01:30:00", dataType = "LocalDateTime")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    @ApiModelProperty(required = true, value = "Filter by the location", position = 5, example = "MDI")
    private String locationId;

    @ApiModelProperty(required = true, value = "Filter by username", position = 6, example = "USER1")
    private String authorUsername;

    @ApiModelProperty(required = true, value = "Filter by a list of case note types and optional case not sub types separated by plus", position = 7, example = "KA+KE,OBS,POM+GEN")
    private List<String> caseNoteTypeSubTypes;

}
