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
@Table(name = "CASE_NOTE_TYPE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"parentType", "type"})
@ToString(of = {"parentType", "type", "description", "active"})
public class SensitiveCaseNoteType {

    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CASE_NOTE_TYPE_ID", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "PARENT_TYPE", nullable = false)
    private ParentNoteType parentType;

    @Column(name = "SUB_TYPE", nullable = false)
    private String type;

    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    @Column(name = "ACTIVE", nullable = false)
    @Builder.Default
    private boolean active = true;

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

    public void update(final String description, boolean active) {
        this.description = description;
        this.active = active;
    }
}
