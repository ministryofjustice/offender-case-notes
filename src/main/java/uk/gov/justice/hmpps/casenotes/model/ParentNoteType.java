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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "CASE_NOTE_PARENT_TYPE")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"type"})
@ToString(of = {"type", "description", "active"})
public class ParentNoteType {

    @Id()
    @Column(name = "NOTE_TYPE", nullable = false)
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

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentType")
    @Builder.Default
    private List<SensitiveCaseNoteType> subTypes = new ArrayList<>();

    public Optional<SensitiveCaseNoteType> getSubType(final String subType) {
        return getSubTypes().stream().filter(t -> t.getType().equalsIgnoreCase(subType)).findFirst();
    }

    public void update(final String description, final boolean active) {
        this.description = description;
        this.active = active;
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

    public List<SensitiveCaseNoteType> getSubTypes() {
        return this.subTypes;
    }
}
