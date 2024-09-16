package uk.gov.justice.hmpps.casenotes.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
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

  @Id
  @Column(nullable = false)
  override val id: UUID,
) : SimpleAudited(), Comparable<Amendment>, AmendmentState {

  @Version
  val version: Long? = null

  override fun compareTo(other: Amendment): Int = createdAt.compareTo(other.createdAt)
}

interface AmendmentRepository : JpaRepository<Amendment, UUID>
