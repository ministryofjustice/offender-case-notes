package uk.gov.justice.hmpps.casenotes.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
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
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "OFFENDER_CASE_NOTE_AMENDMENT")
@Where(clause = "not SOFT_DELETED")
@SQLDelete(sql = "UPDATE offender_case_note_amendment SET soft_deleted = TRUE WHERE offender_case_note_amendment_id = ?", check = ResultCheckStyle.COUNT)
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

    @Builder.Default
    @Setter
    private boolean softDeleted = false;

}
