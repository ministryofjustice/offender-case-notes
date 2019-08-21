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
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"offenderIdentifier", "occurrenceDateTime", "locationId", "staffUsername", "sensitiveCaseNoteType", "noteText"})
@ToString(of = {"id", "offenderIdentifier", "occurrenceDateTime", "locationId", "staffUsername", "sensitiveCaseNoteType"})
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
    private String staffUsername;

    @Column(nullable = false)
    private String staffName;

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

    public OffenderCaseNoteAmendment addAmendment(final String noteText) {
        return addAmendment(noteText, staffUsername, staffName);
    }

    public OffenderCaseNoteAmendment addAmendment(final String noteText, final String staffUsername, final String staffName) {

        final var amendment = OffenderCaseNoteAmendment.builder()
                .caseNote(this)
                .noteText(noteText)
                .staffUsername(staffUsername)
                .staffName(staffName)
                .amendSequence(getLatestSequence() + 1)
                .build();

        amendments.add(amendment);

        return amendment;
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

    public static class AmendmentComparator implements Comparator<OffenderCaseNoteAmendment> {
        @Override
        public int compare(final OffenderCaseNoteAmendment a1, final OffenderCaseNoteAmendment a2) {
            return a1.getAmendSequence() - a2.getAmendSequence();
        }
    }
}
