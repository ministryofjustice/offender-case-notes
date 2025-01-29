package uk.gov.justice.hmpps.casenotes.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.hmpps.casenotes.domain.audit.SimpleAudited
import java.util.UUID

@Entity
@Table(name = "case_note_amendment")
class Amendment(
  @ManyToOne
  @JoinColumn(name = "case_note_id", nullable = false)
  val note: Note,

  @Column(nullable = false)
  override val authorUsername: String,

  @Column(nullable = false)
  override val authorName: String,

  @Column(nullable = false)
  override val authorUserId: String,

  @Column(name = "note_text", nullable = false)
  override val text: String,

  @Enumerated(EnumType.STRING)
  override val system: System,

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private val id: UUID,
) : SimpleAudited(),
  Comparable<Amendment>,
  AmendmentState,
  Persistable<UUID> {

  override fun compareTo(other: Amendment): Int {
    val dif = createdAt.compareTo(other.createdAt)
    return if (dif == 0) {
      id.compareTo(other.id)
    } else {
      dif
    }
  }

  override fun getId(): UUID = id

  @Transient
  private var new: Boolean = true
  override fun isNew(): Boolean = new
}

interface AmendmentRepository : JpaRepository<Amendment, UUID> {
  @Modifying
  @Query("delete from Amendment a where a.id in :ids")
  fun deleteByIdIn(ids: List<UUID>)
}
