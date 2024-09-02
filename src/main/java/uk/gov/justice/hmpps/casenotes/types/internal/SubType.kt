package uk.gov.justice.hmpps.casenotes.types.internal

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType
import uk.gov.justice.hmpps.casenotes.types.SelectableBy

@Immutable
@Entity
@Table(name = "case_note_type")
class SubType(
  @ManyToOne
  @JoinColumn(name = "parent_type", nullable = false)
  val parentType: ParentType,

  @Column(name = "sub_type", nullable = false)
  val code: String,

  @Column(name = "description", nullable = false)
  val description: String,

  @Column(name = "active", nullable = false)
  val active: Boolean = true,

  @Column(name = "sensitive", nullable = false)
  val sensitive: Boolean = true,

  @Column(name = "restricted_use", nullable = false)
  val restrictedUse: Boolean = true,

  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  @Column(name = "case_note_type_id", nullable = false)
  val id: Long? = null,
) {
  @Column(insertable = false, updatable = false)
  val syncToNomis: Boolean = false

  @Column(insertable = false, updatable = false)
  val dpsUserSelectable: Boolean = true
}

fun SubType.toModel(): CaseNoteType =
  CaseNoteType(
    code,
    description,
    active,
    sensitive,
    restrictedUse,
    if (dpsUserSelectable) listOf(SelectableBy.DPS_USER) else listOf(),
  )
