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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static javax.persistence.CascadeType.DETACH;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REFRESH;

@Entity
@Table(name = "OFFENDER_CASE_NOTE")
@Where(clause = "not SOFT_DELETED")
@SQLDelete(sql = "UPDATE offender_case_note SET soft_deleted = TRUE WHERE offender_case_note_id = ?", check = ResultCheckStyle.COUNT)
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
    // cascade All not used as we don't want the soft delete to cascade to the case note amendments in case we need to
    // restore the case note with previously soft deleted amendment
    @OneToMany(cascade = {PERSIST, MERGE, REFRESH, DETACH}, mappedBy = "caseNote")
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

    @Builder.Default
    private boolean softDeleted = false;

    public void addAmendment(final String noteText, final String authorUsername, final String authorName, final String authorUserId) {

        final var amendment = OffenderCaseNoteAmendment.builder()
                .caseNote(this)
                .noteText(noteText)
                .authorUsername(authorUsername)
                .authorName(authorName)
                .authorUserId(authorUserId)
                .build();

        amendments.add(amendment);

        // force modification date change on adding amendment
        modifyDateTime = LocalDateTime.now();
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
            return a1.getCreateDateTime().compareTo(a2.getCreateDateTime());
        }
    }
}
