package uk.gov.justice.hmpps.casenotes.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static jakarta.persistence.CascadeType.DETACH;
import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REFRESH;

@Entity
@Table(name = "OFFENDER_CASE_NOTE")
@Where(clause = "not SOFT_DELETED")
@SQLDelete(sql = "UPDATE offender_case_note SET soft_deleted = TRUE WHERE offender_case_note_id = ?", check = ResultCheckStyle.COUNT)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"offenderIdentifier", "occurrenceDateTime", "locationId", "authorUsername", "caseNoteType", "noteText"})
@ToString(of = {"id", "offenderIdentifier", "occurrenceDateTime", "locationId", "authorUsername", "caseNoteType"})
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
    private CaseNoteType caseNoteType;

    private String noteText;

    @Builder.Default
    @SortComparator(AmendmentComparator.class)
    // cascade All not used as we don't want the soft delete to cascade to the case note amendments in case we need to
    // restore the case note with previously soft deleted amendment
    @OneToMany(cascade = {PERSIST, MERGE, REFRESH, DETACH}, mappedBy = "caseNote")
    private final SortedSet<OffenderCaseNoteAmendment> amendments = new TreeSet<>(new AmendmentComparator());

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

    @Builder.Default
    private boolean softDeleted = false;

    public void addAmendment(final String noteText, final String authorUsername, final String authorName, final String authorUserId) {

        final var amendment = OffenderCaseNoteAmendment.builder()
                .caseNote(this)
                .noteText(noteText)
                .authorUsername(authorUsername)
                .authorName(authorName)
                .authorUserId(authorUserId)
                .createDateTime(now())
                .build();

        amendments.add(amendment);

        // force modification date change on adding amendment
        modifyDateTime = now();
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

    public CaseNoteType getCaseNoteType() {
        return this.caseNoteType;
    }

    public String getNoteText() {
        return this.noteText;
    }

    public SortedSet<OffenderCaseNoteAmendment> getAmendments() {
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
            return a1.getCreateDateTime().compareTo(a2.getCreateDateTime());
        }
    }
}
