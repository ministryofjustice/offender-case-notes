package uk.gov.justice.hmpps.casenotes.domain.audit

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.hmpps.casenotes.domain.AmendmentState
import uk.gov.justice.hmpps.casenotes.domain.DeletionCause
import uk.gov.justice.hmpps.casenotes.domain.IdGenerator.newUuid
import uk.gov.justice.hmpps.casenotes.domain.NoteState
import uk.gov.justice.hmpps.casenotes.domain.System
import java.time.LocalDateTime
import java.util.SortedSet
import java.util.UUID

@Entity
@Table(name = "case_note_deleted")
class DeletedCaseNote(

  val personIdentifier: String,
  val caseNoteId: UUID,
  val legacyId: Long?,

  @JdbcTypeCode(SqlTypes.JSON)
  val caseNote: DeletedDetail,

  val deletedAt: LocalDateTime,
  val deletedBy: String,

  @Enumerated(EnumType.STRING)
  val system: System,

  @Enumerated(EnumType.STRING)
  val cause: DeletionCause,

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private val id: UUID = newUuid(),
) : Persistable<UUID> {
  override fun getId(): UUID = id

  @Transient
  private val new: Boolean = true
  override fun isNew(): Boolean = new
}

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
  override val system: System,
  override val legacyId: Long?,
  private val id: UUID,
  override val createdAt: LocalDateTime,
  override val createdBy: String,
  val amendments: Set<NestedDetail>,
) : NoteState {
  override fun getId(): UUID = id
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
    noteState.system,
    noteState.legacyId,
    noteState.getId(),
    noteState.createdAt,
    noteState.createdBy,
    noteState.amendments().map {
      NestedDetail(
        it.authorUsername,
        it.authorName,
        it.authorUserId,
        it.text,
        it.system,
        it.getId(),
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
  override val system: System,
  private val id: UUID,
  override val createdAt: LocalDateTime,
  override val createdBy: String,
) : AmendmentState, Comparable<NestedDetail> {
  override fun getId(): UUID = id
  override fun compareTo(other: NestedDetail): Int = createdAt.compareTo(other.createdAt)
}
