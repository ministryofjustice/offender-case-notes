package uk.gov.justice.hmpps.casenotes.model;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SortComparator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "OFFENDER_CASE_NOTE")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"offenderIdentifier", "occurrenceDateTime", "locationId", "authorUsername", "sensitiveCaseNoteType", "noteText"})
@ToString(of = {"id", "offenderIdentifier", "occurrenceDateTime", "locationId", "authorUsername", "sensitiveCaseNoteType"})
public class OffenderCaseNote {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "OFFENDER_CASE_NOTE_ID", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime occurrenceDateTime;

    @Column(nullable = false)
    private String offenderIdentifier;

    @Column(nullable = false)
    private String locationId;

    @Column(nullable = false)
    private String authorUsername;

    @Column(nullable = false)
    private String authorUserId;

    @Column(nullable = false)
    private String authorName;

    @ManyToOne
    @JoinColumn(name = "CASE_NOTE_TYPE_ID", nullable = false)
    private SensitiveCaseNoteType sensitiveCaseNoteType;

    private String noteText;

    @Builder.Default
    @SortComparator(AmendmentComparator.class)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "caseNote")
    private final List<OffenderCaseNoteAmendment> amendments = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createDateTime;

    @CreatedBy
    @Column(nullable = false)
    private String createUserId;

    @LastModifiedDate
    private LocalDateTime modifyDateTime;

    @LastModifiedBy
    private String modifyUserId;

    @Column(columnDefinition = "serial", insertable = false, updatable = false)
    private Integer eventId;

    public void addAmendment(final String noteText, final String authorUsername, final String authorName, final String authorUserId) {

        final var amendment = OffenderCaseNoteAmendment.builder()
                .caseNote(this)
                .noteText(noteText)
                .authorUsername(authorUsername)
                .authorName(authorName)
                .authorUserId(authorUserId)
                .amendSequence(getLatestSequence() + 1)
                .build();

        amendments.add(amendment);

        // force modification date change on adding amendment
        modifyDateTime = LocalDateTime.now();
    }

    @NotNull
    private Integer getLatestSequence() {
        return amendments.stream().max(Comparator.comparingInt(OffenderCaseNoteAmendment::getAmendSequence))
                .map(OffenderCaseNoteAmendment::getAmendSequence)
                .orElse(0);
    }

    public Optional<OffenderCaseNoteAmendment> getAmendment(final int sequence) {
        return amendments.stream().filter(a -> a.getAmendSequence() == sequence).findFirst();
    }

    public UUID getId() {
        return this.id;
    }

    public LocalDateTime getOccurrenceDateTime() {
        return this.occurrenceDateTime;
    }

    public String getOffenderIdentifier() {
        return this.offenderIdentifier;
    }

    public String getLocationId() {
        return this.locationId;
    }

    public String getAuthorUsername() {
        return this.authorUsername;
    }

    public String getAuthorUserId() {
        return this.authorUserId;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public SensitiveCaseNoteType getSensitiveCaseNoteType() {
        return this.sensitiveCaseNoteType;
    }

    public String getNoteText() {
        return this.noteText;
    }

    public List<OffenderCaseNoteAmendment> getAmendments() {
        return this.amendments;
    }

    public LocalDateTime getCreateDateTime() {
        return this.createDateTime;
    }

    public String getCreateUserId() {
        return this.createUserId;
    }

    public LocalDateTime getModifyDateTime() {
        return this.modifyDateTime;
    }

    public String getModifyUserId() {
        return this.modifyUserId;
    }

    public Integer getEventId() {
        return this.eventId;
    }

    public static class AmendmentComparator implements Comparator<OffenderCaseNoteAmendment> {
        @Override
        public int compare(final OffenderCaseNoteAmendment a1, final OffenderCaseNoteAmendment a2) {
            return a1.getAmendSequence() - a2.getAmendSequence();
        }
    }
}
