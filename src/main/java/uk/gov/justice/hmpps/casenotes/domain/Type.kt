package uk.gov.justice.hmpps.casenotes.domain

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
class Type(
  @Id @Column(name = "note_type", nullable = false)
  val code: String,

  @Column(name = "description", nullable = false)
  val description: String,

  @OneToMany(cascade = [CascadeType.ALL], mappedBy = "parent")
  private val subTypes: MutableSet<SubType> = mutableSetOf(),
) {
  fun getSubtypes(): Set<SubType> = subTypes.toSet()

  companion object {
    val CODE = Type::code.name
  }
}

interface ParentTypeRepository : JpaRepository<Type, String> {
  @Query(
    """
    select pt from Type pt 
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
  ): List<Type>
}
