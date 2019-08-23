package uk.gov.justice.hmpps.casenotes.model;

import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "CASE_NOTE_PARENT_TYPE")
@Getter
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

    public void update(final String description, boolean active) {
        this.description = description;
        this.active = active;
    }
}
