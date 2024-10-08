package uk.gov.justice.hmpps.casenotes.notes

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.AmendmentState
import uk.gov.justice.hmpps.casenotes.domain.DeletionCause
import uk.gov.justice.hmpps.casenotes.domain.NoteState
import java.time.LocalDateTime
import java.util.SortedSet
import java.util.UUID

@Entity
@Table(name = "case_note_deleted")
@SequenceGenerator(name = "case_note_deleted_id_seq", sequenceName = "case_note_deleted_id_seq", allocationSize = 1)
class DeletedCaseNote(

  val personIdentifier: String,
  val caseNoteId: UUID,
  val legacyId: Long?,

  @JdbcTypeCode(SqlTypes.JSON)
  val caseNote: DeletedDetail,

  val deletedAt: LocalDateTime,
  val deletedBy: String,

  @Enumerated(EnumType.STRING)
  val source: Source,

  @Enumerated(EnumType.STRING)
  val cause: DeletionCause,

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "case_note_deleted_id_seq")
  val id: Long? = null,
)

interface DeletedCaseNoteRepository : JpaRepository<DeletedCaseNote, Long> {
  fun findByCaseNoteId(caseNoteId: UUID): DeletedCaseNote?
}

@JsonNaming(SnakeCaseStrategy::class)
data class DeletedDetail(
  override val personIdentifier: String,
  override val subTypeId: Long,
  override val occurredAt: LocalDateTime,
  override val locationId: String,
  override val authorUsername: String,
  override val authorUserId: String,
  override val authorName: String,
  override val text: String,
  override val systemGenerated: Boolean,
  override val legacyId: Long?,
  override val id: UUID,
  override val createdAt: LocalDateTime,
  override val createdBy: String,
  val amendments: Set<NestedDetail>,
) : NoteState {
  override fun amendments(): SortedSet<out AmendmentState> = amendments.toSortedSet()

  constructor(noteState: NoteState) : this(
    noteState.personIdentifier,
    noteState.subTypeId,
    noteState.occurredAt,
    noteState.locationId,
    noteState.authorUsername,
    noteState.authorUserId,
    noteState.authorName,
    noteState.text,
    noteState.systemGenerated,
    noteState.legacyId,
    noteState.id,
    noteState.createdAt,
    noteState.createdBy,
    noteState.amendments().map {
      NestedDetail(
        it.authorUsername,
        it.authorName,
        it.authorUserId,
        it.text,
        it.id,
        it.createdAt,
        it.createdBy,
      )
    }.toSortedSet(),
  )
}

@JsonNaming(SnakeCaseStrategy::class)
data class NestedDetail(
  override val authorUsername: String,
  override val authorName: String,
  override val authorUserId: String,
  override val text: String,
  override val id: UUID,
  override val createdAt: LocalDateTime,
  override val createdBy: String,
) : AmendmentState, Comparable<NestedDetail> {
  override fun compareTo(other: NestedDetail): Int = createdAt.compareTo(other.createdAt)
}
