package uk.gov.justice.hmpps.casenotes.model;

import lombok.*;
import org.hibernate.annotations.SortComparator;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "OFFENDER_CASE_NOTE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"offenderIdentifier", "occurrenceDateTime", "locationId", "staffUsername", "type", "subType", "noteText"})
@ToString(of = {"id", "offenderIdentifier", "occurrenceDateTime", "locationId", "staffUsername", "type", "subType" })
public class OffenderCaseNote {

    @Id()
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "OFFENDER_CASE_NOTE_ID", nullable = false)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime occurrenceDateTime;

    @Column(nullable = false)
    private String offenderIdentifier;

    @Column(nullable = false)
    private String locationId;

    @Column(nullable = false)
    private String staffUsername;

    @Column(name = "CASE_NOTE_TYPE", nullable = false)
    private String type;

    @Column(name = "CASE_NOTE_SUB_TYPE", nullable = false)
    private String subType;

    private String noteText;

    @Builder.Default
    @SortComparator(AmendmentComparator.class)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "caseNote")
    private List<OffenderCaseNoteAmendment> amendments = new ArrayList<>();

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

    public OffenderCaseNoteAmendment addAmendment(String noteText) {
        return addAmendment(noteText, staffUsername);
    }

    public OffenderCaseNoteAmendment addAmendment(final String noteText, final String staffUsername) {

        final var amendment = OffenderCaseNoteAmendment.builder()
                .caseNote(this)
                .noteText(noteText)
                .staffUsername(staffUsername)
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
        public int compare(OffenderCaseNoteAmendment a1, OffenderCaseNoteAmendment a2) {
            return a1.getAmendSequence() - a2.getAmendSequence();
        }
    }
}
