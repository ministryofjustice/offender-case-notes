package uk.gov.justice.hmpps.casenotes.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import org.hibernate.annotations.SoftDelete
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.domain.IdGenerator.newUuid
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.AMENDMENTS
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.AUTHOR_USERNAME
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.LOCATION_ID
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.OCCURRED_AT
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.PRISON_NUMBER
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.TYPE
import uk.gov.justice.hmpps.casenotes.domain.SubType.Companion.PARENT
import uk.gov.justice.hmpps.casenotes.domain.audit.AuditedEntityListener
import uk.gov.justice.hmpps.casenotes.domain.audit.SimpleAudited
import uk.gov.justice.hmpps.casenotes.notes.TextRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncAmendmentRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncNoteRequest
import java.time.LocalDateTime
import java.util.Optional
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

  type: SubType,

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

  text: String,

  val systemGenerated: Boolean,

  @OneToMany(
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH, CascadeType.REMOVE],
    mappedBy = "note",
    orphanRemoval = true,
  )
  private val amendments: SortedSet<Amendment> = TreeSet(),
) : SimpleAudited() {

  @Version
  val version: Long? = null

  @Id
  @Column(name = "offender_case_note_id", updatable = false, nullable = false)
  val id: UUID = newUuid()

  var legacyId: Long = 0

  @ManyToOne
  @JoinColumn(name = "case_note_type_id", nullable = false)
  var type: SubType = type
    private set

  @Column(name = "note_text", nullable = false)
  var text: String = text
    private set

  fun amendments() = amendments.toSortedSet()
  fun addAmendment(request: TextRequest) = apply {
    if (request is SyncAmendmentRequest) {
      amendments.add(
        Amendment(
          this,
          request.authorUsername,
          request.authorName,
          request.authorUserId,
          request.text,
          newUuid(),
        ),
      )
    } else {
      val context = CaseNoteRequestContext.get()
      amendments.add(
        Amendment(
          this,
          context.username,
          context.userDisplayName,
          context.userId,
          request.text,
          newUuid(),
        ),
      )
    }
  }

  fun sync(request: SyncNoteRequest, typeSupplier: (String, String) -> SubType) = apply {
    if (!(type.code == request.subType && type.parentCode == request.type)) {
      type = typeSupplier(request.type, request.subType)
    }
    text = request.text
    amendments.clear()
    request.amendments.forEach { addAmendment(it) }
  }

  companion object {
    val TYPE = Note::type.name
    val PRISON_NUMBER = Note::prisonNumber.name
    val AUTHOR_USERNAME = Note::authorUsername.name
    val LOCATION_ID = Note::locationId.name
    val OCCURRED_AT = Note::occurredAt.name
    val CREATED_AT = Note::createDateTime.name
    const val AMENDMENTS = "amendments"
  }
}

interface NoteRepository : JpaSpecificationExecutor<Note>, JpaRepository<Note, UUID>, RefreshRepository<Note, UUID> {
  @EntityGraph(attributePaths = ["type.parent", "amendments"])
  fun findByIdAndPrisonNumber(id: UUID, prisonNumber: String): Note?

  @EntityGraph(attributePaths = ["type.parent", "amendments"])
  fun findByLegacyIdAndPrisonNumber(legacyId: Long, prisonNumber: String): Note?

  @EntityGraph(attributePaths = ["type.parent", "amendments"])
  override fun findById(id: UUID): Optional<Note>

  @EntityGraph(attributePaths = ["type.parent", "amendments"])
  fun findByLegacyId(legacyId: Long): Note?

  @Query("select nextval('offender_case_note_event_id_seq')", nativeQuery = true)
  fun getNextLegacyId(): Long
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

fun matchesOnType(includeSensitive: Boolean, typeMap: Map<String, Set<String>>) =
  Specification<Note> { cn, _, cb ->
    @Suppress("UNCHECKED_CAST")
    val subType = cn.fetch<Note, SubType>(TYPE, JoinType.INNER) as Join<Note, SubType>

    @Suppress("UNCHECKED_CAST")
    val parentType = subType.fetch<SubType, Type>(PARENT, JoinType.INNER) as Join<SubType, Type>

    val typePredicate = typeMap.entries.map {
      val matchParent = cb.equal(parentType.get<String>(Type.CODE), it.key)
      if (it.value.isEmpty()) {
        matchParent
      } else {
        cb.and(matchParent, subType.get<String>(SubType.CODE).`in`(it.value))
      }
    }.toTypedArray().let { if (it.isEmpty()) cb.conjunction() else cb.or(*it) }

    val sensitivePredicate = if (includeSensitive) {
      cb.conjunction()
    } else {
      cb.equal(subType.get<Boolean>(SubType.SENSITIVE), false)
    }

    cb.and(typePredicate, sensitivePredicate)
  }
