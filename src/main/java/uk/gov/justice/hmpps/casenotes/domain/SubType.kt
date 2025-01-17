package uk.gov.justice.hmpps.casenotes.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@Immutable
@Entity
@Table(name = "case_note_sub_type")
class SubType(
  @Embedded
  val key: TypeKey,

  @ManyToOne
  @JoinColumn(name = "type_code", nullable = false)
  val type: Type,
  override val code: String,
  val description: String,
  val active: Boolean,
  val sensitive: Boolean,
  val restrictedUse: Boolean,

  @Id
  @Column(name = "id", nullable = false)
  val id: Long? = null,
) : TypeLookup by key {
  @Column(insertable = false, updatable = false)
  val syncToNomis: Boolean = false

  @Column(insertable = false, updatable = false)
  val dpsUserSelectable: Boolean = true

  companion object {
    val CODE = SubType::code.name
    val SENSITIVE = SubType::sensitive.name
    val TYPE = SubType::type.name
  }
}

interface SubTypeRepository : JpaRepository<SubType, Long> {
  @EntityGraph(attributePaths = ["type"])
  fun findByKey(key: TypeKey): SubType?

  @EntityGraph(attributePaths = ["type"])
  fun findByKeyIn(keys: Set<TypeKey>): List<SubType>

  @Query("select st from SubType st join fetch st.type t where t.code in :typeCodes")
  fun findByTypeCodeIn(typeCodes: Set<String>): List<SubType>
}

fun SubTypeRepository.findByTypeCodeAndCode(typeCode: String, code: String) = findByKey(TypeKey(typeCode, code))

fun SubTypeRepository.getByTypeCodeAndCode(typeCode: String, code: String) = findByTypeCodeAndCode(typeCode, code)
  ?: throw IllegalArgumentException("Unknown case note type $typeCode:$code")

interface TypeLookup {
  val typeCode: String
  val code: String
}

@Embeddable
data class TypeKey(
  @Column(name = "type_code", nullable = false, insertable = false, updatable = false)
  override val typeCode: String,
  @Column(name = "code", nullable = false, insertable = false, updatable = false)
  override val code: String,
) : TypeLookup
