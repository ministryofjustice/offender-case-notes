package uk.gov.justice.hmpps.casenotes.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

@Immutable
@Entity
@Table(name = "case_note_type")
class SubType(
  @Embedded
  val key: TypeKey,

  @ManyToOne
  @JoinColumn(name = "parent_type", nullable = false)
  val parent: Type,

  @Column(name = "sub_type", nullable = false)
  override val code: String,

  @Column(name = "description", nullable = false)
  val description: String,

  @Column(name = "active", nullable = false)
  val active: Boolean,

  @Column(name = "sensitive", nullable = false)
  val sensitive: Boolean,

  @Column(name = "restricted_use", nullable = false)
  val restrictedUse: Boolean,

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "case_note_type_id", nullable = false)
  val id: Long? = null,
) : TypeLookup by key {
  @Column(insertable = false, updatable = false)
  val syncToNomis: Boolean = false

  @Column(insertable = false, updatable = false)
  val dpsUserSelectable: Boolean = true

  companion object {
    val CODE = SubType::code.name
    val SENSITIVE = SubType::sensitive.name
    val PARENT = SubType::parent.name
  }
}

interface SubTypeRepository : JpaRepository<SubType, Long> {
  @EntityGraph(attributePaths = ["parent"])
  fun findByKey(key: TypeKey): SubType?

  @EntityGraph(attributePaths = ["parent"])
  fun findByKeyIn(keys: Set<TypeKey>): List<SubType>
}

fun SubTypeRepository.findByParentCodeAndCode(parentCode: String, code: String) =
  findByKey(TypeKey(parentCode, code))

interface TypeLookup {
  val parentCode: String
  val code: String
}

@Embeddable
data class TypeKey(
  @Column(name = "parent_type", nullable = false, insertable = false, updatable = false)
  override val parentCode: String,
  @Column(name = "sub_type", nullable = false, insertable = false, updatable = false)
  override val code: String,
) : TypeLookup
