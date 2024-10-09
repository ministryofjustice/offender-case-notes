package uk.gov.justice.hmpps.casenotes.legacy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Immutable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Immutable
@Entity
@Table(name = "case_note_sub_type")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"type", "code"})
@ToString(of = {"type", "code", "description", "active"})
public class CaseNoteSubType {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "type_code", nullable = false)
    private CaseNoteType type;

    private String code;
    private String description;
    @Builder.Default
    private boolean active = true;
    @Builder.Default
    private boolean sensitive = true;
    @Builder.Default
    private boolean restrictedUse = true;

    @Column(insertable = false, updatable = false)
    private boolean syncToNomis;

    @Column(insertable = false, updatable = false)
    private boolean dpsUserSelectable;
}
