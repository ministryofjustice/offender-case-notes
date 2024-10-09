package uk.gov.justice.hmpps.casenotes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.gov.justice.hmpps.casenotes.domain.AmendmentState;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "case_note_amendment")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString(of = {"id", "caseNote"})
public class OffenderCaseNoteAmendment implements AmendmentState {

    @Id
    @Column(nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "case_note_id", nullable = false)
    private OffenderCaseNote caseNote;

    @Column(nullable = false)
    private String authorUsername;

    @Column(nullable = false)
    private String authorName;

    @Column(nullable = false)
    private String authorUserId;

    @Column(name = "note_text", nullable = false)
    private String text;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(nullable = false)
    private String createdBy;

}
