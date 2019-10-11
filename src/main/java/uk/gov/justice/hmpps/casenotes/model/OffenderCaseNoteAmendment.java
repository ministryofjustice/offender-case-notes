package uk.gov.justice.hmpps.casenotes.model;

import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "OFFENDER_CASE_NOTE_AMENDMENT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString(of = {"id", "amendSequence", "caseNote"})
public class OffenderCaseNoteAmendment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OFFENDER_CASE_NOTE_AMENDMENT_ID", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "OFFENDER_CASE_NOTE_ID", nullable = false)
    private OffenderCaseNote caseNote;

    @Column(nullable = false)
    private int amendSequence;

    @Column(nullable = false)
    private String authorUsername;

    @Column(nullable = false)
    private String authorName;

    @Column(nullable = false)
    private String authorUserId;

    @Column(nullable = false)
    private String noteText;

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

}
