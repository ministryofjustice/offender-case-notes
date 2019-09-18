package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@ApiModel(description = "Case Note Event")
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Data
public class CaseNoteEvents {
    @ApiModelProperty(value = "List of events", required = true, position = 1)
    private List<CaseNoteEvent> events;

    @ApiModelProperty(value = "Date of guaranteed youngest record returned", required = true, position = 2)
    private LocalDateTime latestEventDate;
}
