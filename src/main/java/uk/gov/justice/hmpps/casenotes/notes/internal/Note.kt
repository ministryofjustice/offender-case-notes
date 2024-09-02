package uk.gov.justice.hmpps.casenotes.notes.internal

import com.fasterxml.uuid.Generators
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import org.hibernate.annotations.SoftDelete
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import uk.gov.justice.hmpps.casenotes.audit.AuditedEntityListener
import uk.gov.justice.hmpps.casenotes.audit.SimpleAudited
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.dto.AmendCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.internal.Note.Companion.AMENDMENTS
import uk.gov.justice.hmpps.casenotes.notes.internal.Note.Companion.AUTHOR_USERNAME
import uk.gov.justice.hmpps.casenotes.notes.internal.Note.Companion.LOCATION_ID
import uk.gov.justice.hmpps.casenotes.notes.internal.Note.Companion.OCCURRED_AT
import uk.gov.justice.hmpps.casenotes.notes.internal.Note.Companion.PRISON_NUMBER
import uk.gov.justice.hmpps.casenotes.notes.internal.Note.Companion.TYPE
import uk.gov.justice.hmpps.casenotes.types.internal.ParentType
import java.time.LocalDateTime
import java.util.SortedSet
import java.util.TreeSet
import java.util.UUID

@Entity
@Table(name = "offender_case_note")
@SoftDelete(columnName = "soft_deleted")
@EntityListeners(AuditedEntityListener::class)
class Note(
  @Column(name = "offender_identifier", nullable = false)
  val prisonNumber: String,

  @ManyToOne
  @JoinColumn(name = "case_note_type_id", nullable = false)
  val type: Type,

  @Column(name = "occurrence_date_time", nullable = false)
  val occurredAt: LocalDateTime,

  @Column(nullable = false)
  val locationId: String,

  @Column(nullable = false)
  val authorUsername: String,

  @Column(nullable = false)
  val authorUserId: String,

  @Column(nullable = false)
  val authorName: String,

  @Column(name = "note_text", nullable = false)
  val text: String,

  val systemGenerated: Boolean,

  @OneToMany(
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH],
    mappedBy = "note",
  )
  private val amendments: SortedSet<Amendment> = TreeSet(),
) : SimpleAudited(), Persistable<UUID> {

  @Transient
  var new: Boolean = false
  override fun isNew(): Boolean = new

  @Id
  @Column(name = "offender_case_note_id", updatable = false, nullable = false)
  private val id: UUID = generateNewUuid()
  override fun getId(): UUID = id

  @Column(columnDefinition = "serial", insertable = false, updatable = false)
  var eventId: Int? = null

  var legacyId: Long? = null

  fun amendments() = amendments.toSortedSet()
  fun addAmendment(request: AmendCaseNoteRequest) = apply {
    val context = CaseNoteRequestContext.get()
    amendments.add(Amendment(this, context.username, context.userDisplayName, context.userId, request.text))
  }

  companion object {
    val TYPE = Note::type.name
    val PRISON_NUMBER = Note::prisonNumber.name
    val AUTHOR_USERNAME = Note::authorUsername.name
    val LOCATION_ID = Note::locationId.name
    val OCCURRED_AT = Note::occurredAt.name
    val CREATED_AT = Note::createDateTime.name
    const val AMENDMENTS = "amendments"

    private fun generateNewUuid(): UUID {
      return Generators.timeBasedEpochGenerator().generate()
    }
  }
}

interface NoteRepository : JpaSpecificationExecutor<Note>, JpaRepository<Note, UUID>, RefreshRepository<Note, UUID> {
  @EntityGraph(attributePaths = ["type.category", "amendments"])
  fun findByIdAndPrisonNumber(id: UUID, prisonNumber: String): Note?

  @EntityGraph(attributePaths = ["type.category", "amendments"])
  fun findByLegacyIdAndPrisonNumber(legacyId: Long, prisonNumber: String): Note?
}

fun NoteRepository.saveAndRefresh(note: Note): Note {
  val saved = saveAndFlush(note)
  refresh(saved)
  return saved
}

fun matchesPrisonNumber(prisonNumber: String) =
  Specification<Note> { cn, _, cb ->
    cn.fetch<Note, Amendment>(AMENDMENTS, JoinType.LEFT)
    cb.equal(cb.lower(cn[PRISON_NUMBER]), prisonNumber.lowercase())
  }

fun matchesLocationId(locationId: String) =
  Specification<Note> { cn, _, cb -> cb.equal(cb.lower(cn[LOCATION_ID]), locationId.lowercase()) }

fun matchesAuthorUsername(authorUsername: String) =
  Specification<Note> { cn, _, cb -> cb.equal(cb.lower(cn[AUTHOR_USERNAME]), authorUsername.lowercase()) }

fun occurredBefore(to: LocalDateTime) =
  Specification<Note> { csip, _, cb -> cb.lessThanOrEqualTo(csip[OCCURRED_AT], to) }

fun occurredAfter(from: LocalDateTime) =
  Specification<Note> { csip, _, cb -> cb.greaterThanOrEqualTo(csip[OCCURRED_AT], from) }

fun matchesOnType(includeSensitive: Boolean) =
  Specification<Note> { cn, _, cb ->
    val type = cn.fetch<Note, Type>(TYPE, JoinType.INNER) as Join<Note, Type>
    type.fetch<Type, ParentType>(Type.CATEGORY, JoinType.INNER)
    if (includeSensitive) {
      cb.conjunction()
    } else {
      cb.equal(type.get<Boolean>(Type.SENSITIVE), false)
    }
  }
