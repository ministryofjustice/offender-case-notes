package uk.gov.justice.hmpps.casenotes.types.internal

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType
import uk.gov.justice.hmpps.casenotes.types.CreateSubType
import uk.gov.justice.hmpps.casenotes.types.asActiveYn

@Entity
@Table(name = "case_note_parent_type")
@EntityListeners(AuditedEntityListener::class)
class ParentType(
  @Id @Column(name = "note_type", nullable = false)
  var type: String,

  @Column(name = "description", nullable = false)
  var description: String,

  @OneToMany(cascade = [CascadeType.ALL], mappedBy = "parentType")
  private val subTypes: MutableSet<SubType> = mutableSetOf(),
) : SimpleAudited(), Persistable<String> {
  override fun getId() = type

  @Transient
  var new: Boolean = false
  override fun isNew() = new

  fun isActive() = subTypes.any { it.active }

  fun getSubtypes(): Set<SubType> = subTypes.toSet()

  fun addSubType(createSubType: CreateSubType): SubType {
    if (subTypes.any { it.type == createSubType.type }) {
      throw EntityExistsException(createSubType.type)
    }
    val subType = createSubType.asEntity(this)
    subTypes.add(subType)
    return subType
  }

  fun findSubType(code: String): SubType? = subTypes.find { it.type == code && !it.syncToNomis }
}

fun CreateSubType.asEntity(parent: ParentType) = SubType(parent, type, description, active, sensitive, restrictedUse)

fun ParentType.toModel(): CaseNoteType =
  CaseNoteType(type, description, isActive().asActiveYn(), subCodes = getSubtypes().map(SubType::toModel).sorted())

interface ParentTypeRepository : JpaRepository<ParentType, String> {
  @Query(
    """
    select pt from ParentType pt 
    join fetch pt.subTypes st 
    where (st.active = true or :activeOnly = false) 
    and (:includeSensitive = true or st.sensitive = false) 
    and (:includeRestricted = true or st.restrictedUse = false)
    and (st.dpsUserSelectable = true or :dpsUserSelectableOnly = false)
    """,
  )
  fun findAllWithParams(
    activeOnly: Boolean,
    includeSensitive: Boolean,
    includeRestricted: Boolean,
    dpsUserSelectableOnly: Boolean,
  ): List<ParentType>
}
