package uk.gov.justice.hmpps.casenotes.types.internal

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@Immutable
@Entity
@Table(name = "case_note_parent_type")
class ParentType(
  @Id @Column(name = "note_type", nullable = false)
  val code: String,

  @Column(name = "description", nullable = false)
  val description: String,

  @OneToMany(cascade = [CascadeType.ALL], mappedBy = "parentType")
  private val subTypes: MutableSet<SubType> = mutableSetOf(),
) {
  fun getSubtypes(): Set<SubType> = subTypes.toSet()
}

fun ParentType.toModel(): uk.gov.justice.hmpps.casenotes.types.ParentType =
  uk.gov.justice.hmpps.casenotes.types.ParentType(
    code,
    description,
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
