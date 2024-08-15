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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Immutable
@Entity
@Table(name = "CASE_NOTE_TYPE")
@NoArgsConstructor
@AllArgsConstructor
@Getter
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

    private boolean syncToNomis;
}
