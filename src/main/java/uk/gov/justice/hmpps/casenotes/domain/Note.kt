package uk.gov.justice.hmpps.casenotes.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.domain.IdGenerator.newUuid
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.AUTHOR_USERNAME
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.CREATED_AT
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.ID
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.LOCATION_ID
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.OCCURRED_AT
import uk.gov.justice.hmpps.casenotes.domain.Note.Companion.PERSON_IDENTIFIER
import uk.gov.justice.hmpps.casenotes.domain.audit.DeletedEntityListener
import uk.gov.justice.hmpps.casenotes.domain.audit.SimpleAudited
import uk.gov.justice.hmpps.casenotes.notes.TextRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncAmendmentRequest
import java.time.LocalDateTime
import java.util.Optional
import java.util.SortedSet
import java.util.TreeSet
import java.util.UUID

@Entity
@Table(name = "case_note")
@DynamicUpdate
@EntityListeners(DeletedEntityListener::class)
class Note(
  @Column(nullable = false)
  override val personIdentifier: String,

  @ManyToOne
  @JoinColumn(name = "sub_type_id", nullable = false)
  val subType: SubType,

  occurredAt: LocalDateTime,

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

  @Enumerated(EnumType.STRING)
  override val system: System,

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private val id: UUID = newUuid(),
) : SimpleAudited(), NoteState, Persistable<UUID> {

  override fun getId(): UUID = id

  @Transient
  private var new: Boolean = true
  override fun isNew(): Boolean = new

  @Column(name = "sub_type_id", insertable = false, updatable = false, nullable = false)
  override val subTypeId: Long = subType.id!!

  @Column(nullable = false)
  override var occurredAt: LocalDateTime = occurredAt
    private set

  override var legacyId: Long = 0

  @OneToMany(cascade = [CascadeType.ALL], mappedBy = "note")
  private val amendments: SortedSet<Amendment> = TreeSet()

  override fun amendments() = amendments.toSortedSet()

  @Transient
  var mergedAmendments: SortedSet<Amendment> = TreeSet()
    private set

  fun addAmendment(request: TextRequest) = apply {
    if (request is SyncAmendmentRequest) {
      amendments.add(
        Amendment(
          this,
          request.author.username,
          request.author.fullName(),
          request.author.userId,
          request.text,
          request.system ?: System.NOMIS,
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
          System.DPS,
          newUuid(),
        ).apply { createdAt = context.requestAt },
      )
    }
  }

  fun merge(personIdentifier: String): Note = Note(
    personIdentifier,
    subType,
    occurredAt,
    locationId,
    authorUsername,
    authorUserId,
    authorName,
    text,
    systemGenerated,
    system,
    id,
  ).apply {
    legacyId = this@Note.legacyId
    createdAt = this@Note.createdAt
    createdBy = this@Note.createdBy
    mergedAmendments = this@Note.amendments.map {
      Amendment(
        this,
        it.authorUsername,
        it.authorName,
        it.authorUserId,
        it.text,
        it.system,
        it.id,
      ).apply {
        createdAt = it.createdAt
        createdBy = it.createdBy
      }
    }.toSortedSet()
  }

  fun migrateDates(occurredAt: LocalDateTime, createdAt: LocalDateTime) = apply {
    this.occurredAt = occurredAt
    this.createdAt = createdAt
  }

  companion object {
    val ID = Note::id.name
    val SUB_TYPE = Note::subType.name
    val PERSON_IDENTIFIER = Note::personIdentifier.name
    val AUTHOR_USERNAME = Note::authorUsername.name
    val LOCATION_ID = Note::locationId.name
    val OCCURRED_AT = Note::occurredAt.name
    val CREATED_AT = Note::createdAt.name
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

  @EntityGraph(attributePaths = ["subType.type", "amendments"])
  fun findAllByIdIn(ids: Collection<UUID>): List<Note>

  @EntityGraph(attributePaths = ["subType.type", "amendments"])
  fun findAllByPersonIdentifierAndIdIn(personIdentifier: String, ids: Collection<UUID>): List<Note>

  @EntityGraph(attributePaths = ["subType.type", "amendments"])
  fun findAllByPersonIdentifierAndSubTypeSyncToNomis(personIdentifier: String, syncToNomis: Boolean): List<Note>

  @Query(
    """
    select n from Note n
    join fetch n.subType st
    join fetch st.type
    left join fetch n.amendments a
    where n.personIdentifier = :personIdentifier
    and st.syncToNomis = false
    and (
         ((cast(:from as LocalDateTime) is null or (:from <= n.createdAt))
            and (cast(:to as LocalDateTime) is null or :to >= n.createdAt)) 
         or ((cast(:from as LocalDateTime) is null or :from <= a.createdAt) 
            and (cast(:to as LocalDateTime) is null or :to >= a.createdAt))
        )
    """,
  )
  fun findSarContent(personIdentifier: String, from: LocalDateTime?, to: LocalDateTime?): List<Note>

  @Query(
    """
    select cn from Note cn
    join fetch cn.subType st
    join fetch st.type t
    left join fetch cn.amendments a
    where cn.personIdentifier = :personIdentifier
    and st.syncToNomis = true and cn.legacyId > 0
    """,
  )
  fun findNomisCaseNotesByPersonIdentifier(personIdentifier: String): List<Note>

  @Modifying
  @Query("delete from Note n where n.id in :ids")
  fun deleteByIdIn(ids: List<UUID>)

  fun existsByPersonIdentifierAndSubTypeSensitiveIn(personIdentifier: String, sensitive: Set<Boolean>): Boolean

  @Query(
    """
        select 
            n.personIdentifier  as key, 
            st.key.typeCode     as type, 
            st.key.code         as subType, 
            count(n)            as count, 
            max(n.occurredAt)   as latestAt
        from Note n
        join n.subType st
        where lower(n.personIdentifier) in :personIdentifiers 
        and (st.type.code in :typeCodes or st.key in :typeKeys)
        and (cast(:occurredBefore as timestamp) is null or n.occurredAt <= :occurredBefore) 
        and (cast(:occurredAfter as timestamp) is null or n.occurredAt >= :occurredAfter)
        and (:authorIds is null or n.authorUserId in :authorIds)
        group by n.personIdentifier, st.key.typeCode, st.key.code  
    """,
  )
  fun findUsageByPersonIdentifier(
    personIdentifiers: Set<String>,
    typeCodes: Set<String>,
    typeKeys: Set<TypeKey>,
    occurredAfter: LocalDateTime?,
    occurredBefore: LocalDateTime?,
    authorIds: Set<String>?,
  ): List<UsageCount>

  @Query(
    """
        select 
            n.authorUserId      as key, 
            st.key.typeCode     as type, 
            st.key.code         as subType, 
            count(n)            as count, 
            max(n.occurredAt)   as latestAt
        from Note n
        join n.subType st
        where n.authorUserId in :authorIds 
        and (st.type.code in :typeCodes or st.key in :typeKeys)
        and (cast(:occurredBefore as timestamp) is null or n.occurredAt <= :occurredBefore) 
        and (cast(:occurredAfter as timestamp) is null or n.occurredAt >= :occurredAfter)
        group by n.authorUserId, st.key.typeCode, st.key.code  
    """,
  )
  fun findUsageByAuthorId(
    authorIds: Set<String>,
    typeCodes: Set<String>,
    typeKeys: Set<TypeKey>,
    occurredAfter: LocalDateTime?,
    occurredBefore: LocalDateTime?,
  ): List<UsageCount>

  @Query(
    """
        select 
            n.locationId        as key, 
            st.key.typeCode     as type, 
            st.key.code         as subType, 
            count(n)            as count, 
            max(n.occurredAt)   as latestAt
        from Note n
        join n.subType st
        where lower(n.locationId) in :prisonCodes 
        and (st.type.code in :typeCodes or st.key in :typeKeys)
        and (cast(:occurredBefore as timestamp) is null or n.occurredAt <= :occurredBefore) 
        and (cast(:occurredAfter as timestamp) is null or n.occurredAt >= :occurredAfter)
        group by n.locationId, st.key.typeCode, st.key.code  
    """,
  )
  fun findUsageByPrisonCode(
    prisonCodes: Set<String>,
    typeCodes: Set<String>,
    typeKeys: Set<TypeKey>,
    occurredAfter: LocalDateTime?,
    occurredBefore: LocalDateTime?,
  ): List<UsageCount>
}

interface UsageCount {
  val key: String
  val type: String
  val subType: String
  val count: Int
  val latestAt: LocalDateTime
}

fun NoteRepository.saveAndRefresh(note: Note): Note {
  val saved = saveAndFlush(note)
  refresh(saved)
  return saved
}

fun matchesPersonIdentifier(prisonNumber: String) =
  Specification<Note> { cn, _, cb ->
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

fun createdBetween(from: LocalDateTime, to: LocalDateTime) = Specification<Note> { csip, _, cb ->
  cb.between(csip[CREATED_AT], from, to)
}

fun idIn(ids: Set<UUID>) = Specification<Note> { cn, _, cb ->
  cn.get<String>(ID).`in`(ids)
}
