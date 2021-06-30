package uk.gov.justice.hmpps.casenotes.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
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
@Table(name = "CASE_NOTE_TYPE")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"parentType", "type"})
@ToString(of = {"parentType", "type", "description", "active"})
public class CaseNoteType {

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

    @Column(name = "SENSITIVE", nullable = false)
    @Builder.Default
    private boolean sensitive = true;

    @Column(name = "RESTRICTED_USE", nullable = false)
    @Builder.Default
    private boolean restrictedUse = true;

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

    public void update(final String description, final boolean active, final boolean sensitive, final boolean restrictedUse) {
        this.description = description;
        this.active = active;
        this.sensitive = sensitive;
        this.restrictedUse = restrictedUse;
    }

    public Long getId() {
        return this.id;
    }

    public ParentNoteType getParentType() {
        return this.parentType;
    }

    public String getType() {
        return this.type;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isSensitive() {
        return this.sensitive;
    }

    public boolean isRestrictedUse() {
        return this.restrictedUse;
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
}
