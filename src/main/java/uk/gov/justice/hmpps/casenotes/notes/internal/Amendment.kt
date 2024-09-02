package uk.gov.justice.hmpps.casenotes.notes.internal

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SoftDelete
import uk.gov.justice.hmpps.casenotes.audit.SimpleAudited

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

  @Column(name = "note_text", nullable = false)
  val text: String,

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "offender_case_note_amendment_id", nullable = false)
  val id: Long? = null,
) : SimpleAudited(), Comparable<Amendment> {
  override fun compareTo(other: Amendment): Int = createDateTime.compareTo(other.createDateTime)
}
