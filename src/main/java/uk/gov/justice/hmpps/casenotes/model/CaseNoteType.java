package uk.gov.justice.hmpps.casenotes.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Immutable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Immutable
@Entity
@Table(name = "case_note_type")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"code"})
@ToString(of = {"code", "description"})
public class CaseNoteType {

    @Id
    private String code;

    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "type")
    @Builder.Default
    private List<CaseNoteSubType> subTypes = new ArrayList<>();
}
