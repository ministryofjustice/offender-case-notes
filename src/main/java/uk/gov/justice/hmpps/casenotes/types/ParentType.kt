package uk.gov.justice.hmpps.casenotes.types

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

@Entity
@Table(name = "case_note_parent_type")
@EntityListeners(AuditedEntityListener::class)
class ParentType(
  @Id
  @Column(name = "note_type", nullable = false)
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
}

fun CreateSubType.asEntity(parent: ParentType) = SubType(parent, type, description, active, sensitive, restrictedUse)

interface ParentTypeRepository : JpaRepository<ParentType, String>
