package uk.gov.justice.hmpps.casenotes.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@Immutable
@Entity
@Table(name = "case_note_type")
class Type(
  @Id
  val code: String,
  val description: String,

  @OneToMany(mappedBy = "type")
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
