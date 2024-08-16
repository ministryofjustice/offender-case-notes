package uk.gov.justice.hmpps.casenotes.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Immutable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Immutable
@Entity
@Table(name = "CASE_NOTE_PARENT_TYPE")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"type"})
@ToString(of = {"type", "description"})
public class ParentNoteType {

    @Id()
    @Column(name = "NOTE_TYPE", nullable = false)
    private String type;

    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

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
    private List<CaseNoteType> subTypes = new ArrayList<>();
}
