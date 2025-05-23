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
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.domain.Persistable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
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
import uk.gov.justice.hmpps.casenotes.notes.ReplaceAmendmentRequest
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
) : SimpleAudited(),
  NoteState,
  Persistable<UUID> {

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

  fun findAmendment(id: UUID): Amendment? = amendments.find { it.id == id }
  fun withAmendment(request: ReplaceAmendmentRequest, original: (UUID) -> Amendment): Note = apply {
    amendments.add(original(request.id).amend(this, request.text))
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

interface NoteRepository :
  JpaSpecificationExecutor<Note>,
  JpaRepository<Note, UUID>,
  RefreshRepository<Note, UUID> {
  @EntityGraph(attributePaths = ["subType.type", "amendments"])
  fun findByIdAndPersonIdentifier(id: UUID, personIdentifier: String): Note?

  @EntityGraph(attributePaths = ["subType.type", "amendments"])
  fun findByLegacyIdAndPersonIdentifier(legacyId: Long, personIdentifier: String): Note?

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

  fun existsByPersonIdentifierAndSubTypeSensitiveIn(personIdentifier: String, sensitive: Set<Boolean>): Boolean

  @Query(
    """
        select 
            n.personIdentifier  as key, 
            st.key.typeCode     as type, 
            st.key.code         as subType, 
            count(n)            as count, 
            max(n.createdAt)    as latestAt
        from Note n
        join n.subType st
        where n.personIdentifier in :personIdentifiers 
        and (st.type.code in :typeCodes or st.key in :typeKeys)
        and (cast(:createdBefore as timestamp) is null or n.createdAt <= :createdBefore) 
        and (cast(:createdAfter as timestamp) is null or n.createdAt >= :createdAfter)
        and (:authorIds is null or n.authorUserId in :authorIds)
        and (:prisonCode is null or n.locationId = :prisonCode)
        group by n.personIdentifier, st.key.typeCode, st.key.code  
    """,
  )
  fun findUsageByPersonIdentifierCreatedAt(
    personIdentifiers: Set<String>,
    typeCodes: Set<String>,
    typeKeys: Set<TypeKey>,
    createdAfter: LocalDateTime?,
    createdBefore: LocalDateTime?,
    authorIds: Set<String>?,
    prisonCode: String?,
  ): List<UsageCount>

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
        where n.personIdentifier in :personIdentifiers 
        and (st.type.code in :typeCodes or st.key in :typeKeys)
        and (cast(:occurredBefore as timestamp) is null or n.occurredAt <= :occurredBefore) 
        and (cast(:occurredAfter as timestamp) is null or n.occurredAt >= :occurredAfter)
        and (:authorIds is null or n.authorUserId in :authorIds)
        and (:prisonCode is null or n.locationId = :prisonCode)
        group by n.personIdentifier, st.key.typeCode, st.key.code  
    """,
  )
  fun findUsageByPersonIdentifierOccurredAt(
    personIdentifiers: Set<String>,
    typeCodes: Set<String>,
    typeKeys: Set<TypeKey>,
    occurredAfter: LocalDateTime?,
    occurredBefore: LocalDateTime?,
    authorIds: Set<String>?,
    prisonCode: String?,
  ): List<UsageCount>

  @Query(
    """
        select 
            n.authorUserId      as key, 
            st.key.typeCode     as type, 
            st.key.code         as subType, 
            count(n)            as count, 
            max(n.createdAt)    as latestAt
        from Note n
        join n.subType st
        where n.authorUserId in :authorIds 
        and (st.type.code in :typeCodes or st.key in :typeKeys)
        and (cast(:createdBefore as timestamp) is null or n.createdAt <= :createdBefore) 
        and (cast(:createdAfter as timestamp) is null or n.createdAt >= :createdAfter)
        and (:prisonCode is null or n.locationId = :prisonCode)
        group by n.authorUserId, st.key.typeCode, st.key.code  
    """,
  )
  fun findUsageByAuthorIdCreatedAt(
    authorIds: Set<String>,
    typeCodes: Set<String>,
    typeKeys: Set<TypeKey>,
    createdAfter: LocalDateTime?,
    createdBefore: LocalDateTime?,
    prisonCode: String?,
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
        and (:prisonCode is null or n.locationId = :prisonCode)
        group by n.authorUserId, st.key.typeCode, st.key.code  
    """,
  )
  fun findUsageByAuthorIdOccurredAt(
    authorIds: Set<String>,
    typeCodes: Set<String>,
    typeKeys: Set<TypeKey>,
    occurredAfter: LocalDateTime?,
    occurredBefore: LocalDateTime?,
    prisonCode: String?,
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

fun matchesPersonIdentifier(personIdentifier: String) = Specification<Note> { cn, _, cb ->
  cb.equal(cn.get<String>(PERSON_IDENTIFIER), personIdentifier)
}

fun matchesLocationId(locationId: String) = Specification<Note> { cn, _, cb -> cb.equal(cn.get<String>(LOCATION_ID), locationId) }

fun matchesAuthorUsername(authorUsername: String) = Specification<Note> { cn, _, cb -> cb.equal(cn.get<String>(AUTHOR_USERNAME), authorUsername) }

fun occurredBefore(to: LocalDateTime) = Specification<Note> { csip, _, cb -> cb.lessThanOrEqualTo(csip[OCCURRED_AT], to) }

fun occurredAfter(from: LocalDateTime) = Specification<Note> { csip, _, cb -> cb.greaterThanOrEqualTo(csip[OCCURRED_AT], from) }

fun matchesOnType(includeSensitive: Boolean, typeMap: Map<String, Set<String>>) = Specification<Note> { cn, q, cb ->
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

fun createdBetween(from: LocalDateTime, to: LocalDateTime, includeSyncToNomis: Boolean) = Specification { cn, _, cb ->
  if (!includeSyncToNomis) {
    val subType = cn.join<Note, SubType>(Note.SUB_TYPE, JoinType.INNER)
    cb.and(cb.createdIncludingAmendments(from, to, cn), cb.equal(subType.get<Boolean>(SubType.SYNC_TO_NOMIS), false))
  } else {
    cb.createdIncludingAmendments(from, to, cn)
  }
}

private fun CriteriaBuilder.createdIncludingAmendments(
  from: LocalDateTime,
  to: LocalDateTime,
  cn: Root<Note>,
): Predicate {
  val cnBetween = between(cn[CREATED_AT], from, to)
  val amendment = cn.join<Note, Amendment>("amendments", JoinType.LEFT)
  val amBetween = between(amendment[CREATED_AT], from, to)
  return or(cnBetween, amBetween)
}

fun idIn(ids: Set<UUID>) = Specification<Note> { cn, _, cb ->
  cn.get<String>(ID).`in`(ids)
}
