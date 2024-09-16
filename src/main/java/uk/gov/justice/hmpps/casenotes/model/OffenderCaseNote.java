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
import jakarta.persistence.Version;
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
@Table(name = "case_note")
@SoftDelete(columnName = "soft_deleted")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"personIdentifier", "occurredAt", "locationId", "authorUsername", "caseNoteType", "noteText"})
@ToString(of = {"id", "personIdentifier", "occurredAt", "locationId", "authorUsername", "caseNoteType"})
@SQLRestriction("exists(select 1 from case_note_type ct where ct.case_note_type_id = type_id and ct.sync_to_nomis = false)")
public class OffenderCaseNote {

    @Builder.Default
    @Id
    @Column(updatable = false, nullable = false)
    private UUID id = generateNewUuid();

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false)
    private String personIdentifier;

    @Column(nullable = false)
    private String locationId;

    @Column(nullable = false)
    private String authorUsername;

    @Column(nullable = false)
    private String authorUserId;

    @Column(nullable = false)
    private String authorName;

    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
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
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(nullable = false)
    private String createdBy;

    @Column(columnDefinition = "serial", insertable = false, updatable = false)
    private Long legacyId;

    private boolean systemGenerated;

    @Version
    private Long version;

    public void addAmendment(
        final String noteText,
        final String authorUsername,
        final String authorName,
        final String authorUserId
    ) {

        final var amendment = OffenderCaseNoteAmendment.builder()
            .caseNote(this)
            .noteText(noteText)
            .authorUsername(authorUsername)
            .authorName(authorName)
            .authorUserId(authorUserId)
            .createdAt(now())
            .id(generateNewUuid())
            .build();

        amendments.add(amendment);
    }

    public static class AmendmentComparator implements Comparator<OffenderCaseNoteAmendment> {
        @Override
        public int compare(final OffenderCaseNoteAmendment a1, final OffenderCaseNoteAmendment a2) {
            return a1.getCreatedAt().compareTo(a2.getCreatedAt());
        }
    }

    private static UUID generateNewUuid() {
        return Generators.timeBasedEpochGenerator().generate();
    }
}
