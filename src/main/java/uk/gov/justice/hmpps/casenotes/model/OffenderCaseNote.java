package uk.gov.justice.hmpps.casenotes.model;

import com.fasterxml.uuid.Generators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SortComparator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import static jakarta.persistence.CascadeType.DETACH;
import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REFRESH;
import static java.time.LocalDateTime.now;

@Entity
@Table(name = "OFFENDER_CASE_NOTE")
@SoftDelete(columnName = "soft_deleted")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"offenderIdentifier", "occurrenceDateTime", "locationId", "authorUsername", "caseNoteType", "noteText"})
@ToString(of = {"id", "offenderIdentifier", "occurrenceDateTime", "locationId", "authorUsername", "caseNoteType"})
@SQLRestriction("exists(select 1 from case_note_type ct where ct.case_note_type_id = case_note_type_id and ct.sync_to_nomis = false)")
public class OffenderCaseNote {

    @Builder.Default
    @Id
    @Column(name = "OFFENDER_CASE_NOTE_ID", updatable = false, nullable = false)
    private UUID id = generateNewUuid();

    @Column(name = "occurrence_date_time", nullable = false)
    private LocalDateTime occurrenceDateTime;

    @Column(name = "offender_identifier", nullable = false)
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

    @Column(name = "note_text", nullable = false)
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

    private boolean systemGenerated;

    private Long legacyId;

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

    public static class AmendmentComparator implements Comparator<OffenderCaseNoteAmendment> {
        @Override
        public int compare(final OffenderCaseNoteAmendment a1, final OffenderCaseNoteAmendment a2) {
            return a1.getCreateDateTime().compareTo(a2.getCreateDateTime());
        }
    }

    private static UUID generateNewUuid() {
        return Generators.timeBasedEpochGenerator().generate();
    }
}
