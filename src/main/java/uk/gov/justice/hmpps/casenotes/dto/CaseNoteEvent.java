package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNoteAmendment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


@Getter
@ApiModel(description = "Case Note Event")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Data
public class CaseNoteEvent {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    @ApiModelProperty(name = "noms_id", value = "Offender Noms Id", example = "A1417AE", required = true, position = 1)
    private String nomsId;

    @ApiModelProperty(required = true, value = "Case Note Id (unique)", example = "12311312", position = 2)
    private String id;

    @ApiModelProperty(required = true, value = "Case Note Text", position = 3, example = "This is some text")
    private String content;

    @ApiModelProperty(required = true, value = "Date and Time of when case note contact with offender was made", position = 4, example = "2017-10-31T01:30:00")
    private LocalDateTime contactTimestamp;

    @ApiModelProperty(required = true, value = "Date and Time of notification of event", position = 5, example = "2017-10-31T01:30:00")
    private LocalDateTime notificationTimestamp;

    @ApiModelProperty(required = true, value = "Name of staff member who created case note (lastname, firstname)", position = 6, example = "Smith, John")
    private String staffName;

    @ApiModelProperty(value = "Agency Code where Case Note was made.", position = 7, example = "MDI")
    private String establishmentCode;

    @ApiModelProperty(required = true, value = "Case Note Type and Sub Type", position = 8, example = "POS IEP_ENC")
    private String noteType;

    public CaseNoteEvent(final OffenderCaseNote o) {
        nomsId = o.getOffenderIdentifier();
        id = o.getId().toString();
        content = constructNoteTextWithAmendments(o.getNoteText(), o.getAmendments());
        contactTimestamp = o.getOccurrenceDateTime();
        notificationTimestamp = o.getModifyDateTime();
        staffName = o.getAuthorName();
        establishmentCode = o.getLocationId();
        noteType = o.getSensitiveCaseNoteType().getParentType().getType() + " " + o.getSensitiveCaseNoteType().getType();
    }

    private String constructNoteTextWithAmendments(final String noteText, final List<OffenderCaseNoteAmendment> amendments) {
        // need format This is a case note ...[PPHILLIPS_ADM updated the case notes on 2019/09/04 11:20:11] Amendment to case note ...[PPHILLIPS_ADM updated the case notes on 2019/09/09 09:23:42] another amendment
        return noteText + amendments.stream().map(a -> String.format(" ...[%s updated the case notes on %s] %s",
                a.getAuthorUsername(), dtf.format(a.getCreateDateTime()), a.getNoteText())).collect(Collectors.joining());
    }
}
