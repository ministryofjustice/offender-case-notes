package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@ApiModel(description = "Case Note")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class CaseNote {

    @ApiModelProperty(required = true, value = "Case Note Id (unique)", position = 1, example = "12311312")
    @NotNull
    private String caseNoteId;

    @ApiModelProperty(required = true, value = "Offender Unique Identifier", position = 2, example = "A1234AA")
    @NotNull
    private String offenderIdentifier;

    @ApiModelProperty(required = true, value = "Case Note Type", position = 3, example = "KA")
    @NotBlank
    private String type;

    @ApiModelProperty(required = true, value = "Case Note Type Description", position = 4, example = "Key Worker")
    @NotBlank
    private String typeDescription;

    @ApiModelProperty(required = true, value = "Case Note Sub Type", position = 5, example = "KS")
    @NotBlank
    private String subType;

    @ApiModelProperty(required = true, value = "Case Note Sub Type Description", position = 6, example = "Key Worker Session")
    @NotBlank
    private String subTypeDescription;

    @ApiModelProperty(required = true, value = "Source Type", position = 7, example = "INST")
    @NotBlank
    private String source;

    @ApiModelProperty(required = true, value = "Date and Time of Case Note creation", position = 8, example = "2017-10-31T01:30:00")
    @NotNull
    private LocalDateTime creationDateTime;

    @ApiModelProperty(required = true, value = "Date and Time of when case note contact with offender was made", position = 9, example = "2017-10-31T01:30:00")
    @NotNull
    private LocalDateTime occurrenceDateTime;

    @ApiModelProperty(required = true, value = "Full name of case note author", position = 11, example = "John Smith")
    @NotNull
    private String authorName;

    @ApiModelProperty(required = true, value = "User Id of case note author - staffId for nomis users, userId for auth users", position = 11, example = "12345")
    @NotNull
    private String authorUserId;

    @ApiModelProperty(required = true, value = "Case Note Text", position = 13, example = "This is some text")
    @NotBlank
    private String text;

    @ApiModelProperty(value = "Location Id representing where Case Note was made.", position = 14, example = "MDI")
    private String locationId;

    @ApiModelProperty(required = true, value = "Delius number representation of the case note id - will be negative for sensitive case note types", position = 15, example = "-23")
    @NotNull
    private Integer eventId;

    @ApiModelProperty(required = true, value = "Ordered list of amendments to the case note (oldest first)", position = 15)
    @NotNull
    @Builder.Default
    private List<CaseNoteAmendment> amendments = new ArrayList<>();

    public @NotNull String getCaseNoteId() {
        return this.caseNoteId;
    }

    public @NotNull String getOffenderIdentifier() {
        return this.offenderIdentifier;
    }

    public @NotBlank String getType() {
        return this.type;
    }

    public @NotBlank String getTypeDescription() {
        return this.typeDescription;
    }

    public @NotBlank String getSubType() {
        return this.subType;
    }

    public @NotBlank String getSubTypeDescription() {
        return this.subTypeDescription;
    }

    public @NotBlank String getSource() {
        return this.source;
    }

    public @NotNull LocalDateTime getCreationDateTime() {
        return this.creationDateTime;
    }

    public @NotNull LocalDateTime getOccurrenceDateTime() {
        return this.occurrenceDateTime;
    }

    public @NotNull String getAuthorName() {
        return this.authorName;
    }

    public @NotNull String getAuthorUserId() {
        return this.authorUserId;
    }

    public @NotBlank String getText() {
        return this.text;
    }

    public String getLocationId() {
        return this.locationId;
    }

    public @NonNull Integer getEventId() {
        return eventId;
    }

    public @NotNull List<CaseNoteAmendment> getAmendments() {
        return this.amendments;
    }
}
