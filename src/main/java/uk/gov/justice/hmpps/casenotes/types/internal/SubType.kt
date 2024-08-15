package uk.gov.justice.hmpps.casenotes.types.internal

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType
import uk.gov.justice.hmpps.casenotes.types.asActiveYn

@Entity
@Table(name = "case_note_type")
@EntityListeners(AuditedEntityListener::class)
class SubType(
  @ManyToOne
  @JoinColumn(name = "parent_type", nullable = false)
  var parentType: ParentType,

  @Column(name = "sub_type", nullable = false)
  var type: String,

  @Column(name = "description", nullable = false)
  var description: String,

  @Column(name = "active", nullable = false)
  var active: Boolean = true,

  @Column(name = "sensitive", nullable = false)
  var sensitive: Boolean = true,

  @Column(name = "restricted_use", nullable = false)
  var restrictedUse: Boolean = true,

  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  @Column(name = "case_note_type_id", nullable = false)
  val id: Long? = null,
) : SimpleAudited() {
  @Column(insertable = false, updatable = false)
  val syncToNomis: Boolean = false

  @Column(insertable = false, updatable = false)
  val dpsUserSelectable: Boolean = true
}

fun SubType.toModel(): CaseNoteType =
  CaseNoteType(type, description, active.asActiveYn(), SOURCE, sensitive, restrictedUse)
