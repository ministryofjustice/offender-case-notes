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
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.PERSON_IDENTIFIER
import uk.gov.justice.hmpps.casenotes.domain.audit.DeletedEntityListener
import uk.gov.justice.hmpps.casenotes.domain.audit.SimpleAudited
import uk.gov.justice.hmpps.casenotes.notes.TextRequest
import uk.gov.justice.hmpps.casenotes.sync.MigrationResult
import uk.gov.justice.hmpps.casenotes.sync.SyncAmendmentRequest
import java.time.LocalDateTime
import java.util.Optional
import java.util.SortedSet
import java.util.TreeSet
import java.util.UUID

@Entity
@Table(name = "case_note")
@EntityListeners(DeletedEntityListener::class)
class Note(
  @Column(nullable = false)
  override val personIdentifier: String,

  @ManyToOne
  @JoinColumn(name = "sub_type_id", nullable = false)
  val subType: SubType,

  @Column(nullable = false)
  override val occurredAt: LocalDateTime,

  @Column(nullable = false)
  override val locationId: String,

  @Column(nullable = false)
  override val authorUsername: String,

  @Column(nullable = false)
  override val authorUserId: String,

  @Column(nullable = false)
  override val authorName: String,

  @Column(name = "note_text", nullable = false)
  override val text: String,

  override val systemGenerated: Boolean,
) : SimpleAudited(), NoteState {

  @Version
  val version: Long? = null

  @Id
  @Column(updatable = false, nullable = false)
  override val id: UUID = newUuid()

  @Column(name = "sub_type_id", insertable = false, updatable = false, nullable = false)
  override val subTypeId: Long = subType.id!!
  override var legacyId: Long = 0

  @OneToMany(cascade = [CascadeType.ALL], mappedBy = "note")
  private val amendments: SortedSet<Amendment> = TreeSet()

  override fun amendments() = amendments.toSortedSet()

  fun addAmendment(request: TextRequest) = apply {
    if (request is SyncAmendmentRequest) {
      amendments.add(
        Amendment(
          this,
          request.author.username,
          request.author.fullName(),
          request.author.userId,
          request.text,
          newUuid(),
        ).apply { createdAt = request.createdDateTime },
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
        ).apply { createdAt = context.requestAt },
      )
    }
  }

  companion object {
    val SUB_TYPE = Note::subType.name
    val PERSON_IDENTIFIER = Note::personIdentifier.name
    val AUTHOR_USERNAME = Note::authorUsername.name
    val LOCATION_ID = Note::locationId.name
    val OCCURRED_AT = Note::occurredAt.name
    val CREATED_AT = Note::createdAt.name
    const val AMENDMENTS = "amendments"
  }
}

interface NoteRepository : JpaSpecificationExecutor<Note>, JpaRepository<Note, UUID>, RefreshRepository<Note, UUID> {
  @EntityGraph(attributePaths = ["subType.type", "amendments"])
  fun findByIdAndPersonIdentifier(id: UUID, prisonNumber: String): Note?

  @EntityGraph(attributePaths = ["subType.type", "amendments"])
  fun findByLegacyIdAndPersonIdentifier(legacyId: Long, prisonNumber: String): Note?

  @EntityGraph(attributePaths = ["subType.type", "amendments"])
  override fun findById(id: UUID): Optional<Note>

  @EntityGraph(attributePaths = ["subType.type", "amendments"])
  fun findByLegacyId(legacyId: Long): Note?

  @Query("select nextval('case_note_legacy_id_seq')", nativeQuery = true)
  fun getNextLegacyId(): Long

  @Query("select new uk.gov.justice.hmpps.casenotes.sync.MigrationResult(n.id, n.legacyId) from Note n where n.legacyId in (:legacyIds)")
  fun findMigratedIds(legacyIds: List<Long>): List<MigrationResult>
}

fun NoteRepository.saveAndRefresh(note: Note): Note {
  val saved = saveAndFlush(note)
  refresh(saved)
  return saved
}

fun matchesPrisonNumber(prisonNumber: String) =
  Specification<Note> { cn, q, cb ->
    if (q.resultType == cn.javaType) {
      cn.join<Note, Amendment>(AMENDMENTS, JoinType.LEFT)
    }
    cb.equal(cb.lower(cn[PERSON_IDENTIFIER]), prisonNumber.lowercase())
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
  Specification<Note> { cn, q, cb ->
    val (type, subType) = if (q.resultType == cn.javaType) {
      @Suppress("UNCHECKED_CAST")
      val subType = cn.fetch<Note, SubType>(Note.SUB_TYPE, JoinType.INNER) as Join<Note, SubType>

      @Suppress("UNCHECKED_CAST")
      val type = subType.fetch<SubType, Type>(SubType.TYPE, JoinType.INNER) as Join<SubType, Type>
      (type to subType)
    } else {
      val subType = cn.join<Note, SubType>(Note.SUB_TYPE, JoinType.INNER)
      val type = subType.join<SubType, Type>(SubType.TYPE, JoinType.INNER)
      (type to subType)
    }

    val typePredicate = typeMap.entries.map {
      val matchParent = cb.equal(type.get<String>(Type.CODE), it.key)
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
