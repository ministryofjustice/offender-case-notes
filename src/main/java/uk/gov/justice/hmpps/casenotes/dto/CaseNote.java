package uk.gov.justice.hmpps.casenotes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@ApiModel(description = "Case Note")
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    @ApiModelProperty(required = true, value = "Sensitive Note", position = 16, example = "true")
    private boolean sensitive;

    @ApiModelProperty(required = true, value = "Ordered list of amendments to the case note (oldest first)", position = 17)
    @NotNull
    private List<CaseNoteAmendment> amendments = new ArrayList<>();

    public static CaseNoteBuilder builder() {
        return new CaseNoteBuilder();
    }

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

    public boolean getSensitive() {
        return sensitive;
    }

    public @NotNull List<CaseNoteAmendment> getAmendments() {
        return this.amendments;
    }

    public static class CaseNoteBuilder {
        private @NotNull String caseNoteId;
        private @NotNull String offenderIdentifier;
        private @NotBlank String type;
        private @NotBlank String typeDescription;
        private @NotBlank String subType;
        private @NotBlank String subTypeDescription;
        private @NotBlank String source;
        private @NotNull LocalDateTime creationDateTime;
        private @NotNull LocalDateTime occurrenceDateTime;
        private @NotNull String authorName;
        private @NotNull String authorUserId;
        private @NotBlank String text;
        private String locationId;
        private @NotNull Integer eventId;
        private boolean sensitive;
        private @NotNull List<CaseNoteAmendment> amendments;

        CaseNoteBuilder() {
        }

        public CaseNoteBuilder caseNoteId(@NotNull final String caseNoteId) {
            this.caseNoteId = caseNoteId;
            return this;
        }

        public CaseNoteBuilder offenderIdentifier(@NotNull final String offenderIdentifier) {
            this.offenderIdentifier = offenderIdentifier;
            return this;
        }

        public CaseNoteBuilder type(@NotBlank final String type) {
            this.type = type;
            return this;
        }

        public CaseNoteBuilder typeDescription(@NotBlank final String typeDescription) {
            this.typeDescription = typeDescription;
            return this;
        }

        public CaseNoteBuilder subType(@NotBlank final String subType) {
            this.subType = subType;
            return this;
        }

        public CaseNoteBuilder subTypeDescription(@NotBlank final String subTypeDescription) {
            this.subTypeDescription = subTypeDescription;
            return this;
        }

        public CaseNoteBuilder source(@NotBlank final String source) {
            this.source = source;
            return this;
        }

        public CaseNoteBuilder creationDateTime(@NotNull final LocalDateTime creationDateTime) {
            this.creationDateTime = creationDateTime;
            return this;
        }

        public CaseNoteBuilder occurrenceDateTime(@NotNull final LocalDateTime occurrenceDateTime) {
            this.occurrenceDateTime = occurrenceDateTime;
            return this;
        }

        public CaseNoteBuilder authorName(@NotNull final String authorName) {
            this.authorName = authorName;
            return this;
        }

        public CaseNoteBuilder authorUserId(@NotNull final String authorUserId) {
            this.authorUserId = authorUserId;
            return this;
        }

        public CaseNoteBuilder text(@NotBlank final String text) {
            this.text = text;
            return this;
        }

        public CaseNoteBuilder locationId(final String locationId) {
            this.locationId = locationId;
            return this;
        }

        public CaseNoteBuilder eventId(@NotNull final Integer eventId) {
            this.eventId = eventId;
            return this;
        }

        public CaseNoteBuilder sensitive(final boolean sensitive) {
            this.sensitive = sensitive;
            return this;
        }

        public CaseNoteBuilder amendments(@NotNull final List<CaseNoteAmendment> amendments) {
            this.amendments = amendments;
            return this;
        }

        public CaseNote build() {
            return new CaseNote(caseNoteId, offenderIdentifier, type, typeDescription, subType, subTypeDescription, source, creationDateTime, occurrenceDateTime, authorName, authorUserId, text, locationId, eventId, sensitive, amendments);
        }

        public String toString() {
            return "CaseNote.CaseNoteBuilder(caseNoteId=" + this.caseNoteId + ", offenderIdentifier=" + this.offenderIdentifier + ", type=" + this.type + ", typeDescription=" + this.typeDescription + ", subType=" + this.subType + ", subTypeDescription=" + this.subTypeDescription + ", source=" + this.source + ", creationDateTime=" + this.creationDateTime + ", occurrenceDateTime=" + this.occurrenceDateTime + ", authorName=" + this.authorName + ", authorUserId=" + this.authorUserId + ", text=" + this.text + ", locationId=" + this.locationId + ", eventId=" + this.eventId + ", sensitive=" + this.sensitive + ", amendments=" + this.amendments + ")";
        }
    }
}
