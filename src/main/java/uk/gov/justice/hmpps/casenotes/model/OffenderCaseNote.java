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
import org.hibernate.annotations.SortComparator;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.gov.justice.hmpps.casenotes.domain.NoteState;
import uk.gov.justice.hmpps.casenotes.domain.audit.DeletedEntityListener;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static java.time.LocalDateTime.now;

@Entity
@Table(name = "case_note")
@EntityListeners({AuditingEntityListener.class, DeletedEntityListener.class})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"personIdentifier", "occurredAt", "locationId", "authorUsername", "subType", "text"})
@ToString(of = {"id", "personIdentifier", "occurredAt", "locationId", "authorUsername", "subType"})
@SQLRestriction("exists(select 1 from case_note_sub_type ct where ct.id = sub_type_id and ct.sync_to_nomis = false)")
public class OffenderCaseNote implements NoteState {

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
    @JoinColumn(name = "sub_type_id", nullable = false)
    private CaseNoteSubType subType;

    @Column(name = "note_text", nullable = false)
    private String text;

    @Builder.Default
    @SortComparator(AmendmentComparator.class)
    @OneToMany(cascade = ALL, mappedBy = "caseNote")
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

    @Override
    public boolean getSystemGenerated() {
        return systemGenerated;
    }

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
            .text(noteText)
            .authorUsername(authorUsername)
            .authorName(authorName)
            .authorUserId(authorUserId)
            .createdAt(now())
            .id(generateNewUuid())
            .build();

        amendments.add(amendment);
    }

    @Override
    public long getSubTypeId() {
        return subType.getId();
    }

    @NotNull
    @Override
    public SortedSet<OffenderCaseNoteAmendment> amendments() {
        return Collections.unmodifiableSortedSet(amendments);
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
