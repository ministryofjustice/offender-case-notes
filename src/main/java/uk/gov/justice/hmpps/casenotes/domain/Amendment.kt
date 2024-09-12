package uk.gov.justice.hmpps.casenotes.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.SoftDelete
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.hmpps.casenotes.domain.audit.SimpleAudited
import uk.gov.justice.hmpps.casenotes.notes.TextRequest
import java.util.UUID

@Entity
@SoftDelete(columnName = "soft_deleted")
@Table(name = "offender_case_note_amendment")
class Amendment(
  @ManyToOne
  @JoinColumn(name = "offender_case_note_id", nullable = false)
  val note: Note,

  @Column(nullable = false)
  val authorUsername: String,

  @Column(nullable = false)
  val authorName: String,

  @Column(nullable = false)
  val authorUserId: String,

  text: String,

  @Id
  @Column(name = "offender_case_note_amendment_id", nullable = false)
  val id: UUID,
) : SimpleAudited(), Comparable<Amendment> {

  @Version
  val version: Long? = null

  override fun compareTo(other: Amendment): Int = createDateTime.compareTo(other.createDateTime)

  @Column(name = "note_text", nullable = false)
  var text: String = text
    private set

  internal fun update(request: TextRequest) = apply {
    text = request.text
  }
}

interface AmendmentRepository : JpaRepository<Amendment, UUID>
