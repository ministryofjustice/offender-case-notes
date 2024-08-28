package uk.gov.justice.hmpps.casenotes.types.internal

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType

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
}

fun ParentType.toModel(): CaseNoteType =
  CaseNoteType(
    type,
    description,
    isActive(),
    sensitive = false,
    restrictedUse = false,
    selectableBy = listOf(),
    subCodes = getSubtypes().map(SubType::toModel).sorted(),
  )

interface ParentTypeRepository : JpaRepository<ParentType, String> {
  @Query(
    """
    select pt from ParentType pt 
    join fetch pt.subTypes st 
    where (:includeInactive = true or st.active = true)
    and (:includeRestricted = true or st.restrictedUse = false)
    and (:dpsUserSelectableOnly = false or st.dpsUserSelectable = true)
    """,
  )
  fun findAllWithParams(
    includeInactive: Boolean,
    includeRestricted: Boolean,
    dpsUserSelectableOnly: Boolean,
  ): List<ParentType>
}
