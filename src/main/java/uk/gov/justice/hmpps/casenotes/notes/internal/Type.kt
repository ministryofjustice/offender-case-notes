package uk.gov.justice.hmpps.casenotes.notes.internal

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.hmpps.casenotes.audit.AuditedEntityListener
import uk.gov.justice.hmpps.casenotes.audit.SimpleAudited

@Immutable
@Entity
@Table(name = "case_note_type")
@EntityListeners(AuditedEntityListener::class)
class Type(
  @ManyToOne
  @JoinColumn(name = "parent_type", nullable = false)
  val category: Category,

  @Column(name = "sub_type", nullable = false)
  val code: String,

  @Column(name = "description", nullable = false)
  val description: String,

  @Column(name = "sensitive", nullable = false)
  val sensitive: Boolean = true,

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "case_note_type_id", nullable = false)
  val id: Long,
) {
  companion object {
    val CODE = Type::code.name
    val SENSITIVE = Type::sensitive.name
    val CATEGORY = Type::category.name
  }
}

@Immutable
@Entity
@Table(name = "case_note_parent_type")
@EntityListeners(AuditedEntityListener::class)
class Category(
  @Id @Column(name = "note_type", nullable = false)
  val code: String,

  @Column(name = "description", nullable = false)
  val description: String,
) : SimpleAudited() {
  companion object {
    val CODE = Category::code.name
  }
}
