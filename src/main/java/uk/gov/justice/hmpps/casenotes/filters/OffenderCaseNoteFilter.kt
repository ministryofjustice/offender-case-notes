package uk.gov.justice.hmpps.casenotes.filters

import com.google.common.collect.ImmutableList
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import java.time.LocalDateTime
import java.util.function.Consumer

class OffenderCaseNoteFilter(
  internal val offenderIdentifier: String? = null,
  internal val locationId: String? = null,
  internal val authorUsername: String? = null,
  internal val excludeSensitive: Boolean = false,
  internal val startDate: LocalDateTime? = null,
  internal val endDate: LocalDateTime? = null,
  internal val typeSubTypes: Map<String, Set<String>> = emptyMap(),
) : Specification<OffenderCaseNote> {

  override fun toPredicate(root: Root<OffenderCaseNote>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate? {
    val predicateBuilder = mutableListOf<Predicate>()

    if (!offenderIdentifier.isNullOrBlank()) {
      predicateBuilder.add(cb.equal(root.get<Any>("offenderIdentifier"), offenderIdentifier))
    }
    if (!locationId.isNullOrBlank()) {
      predicateBuilder.add(cb.equal(root.get<Any>("locationId"), locationId))
    }
    if (!authorUsername.isNullOrBlank()) {
      predicateBuilder.add(cb.equal(root.get<Any>("authorUsername"), authorUsername))
    }
    if (excludeSensitive) {
      val caseNoteType: Join<Any, Any> = root.join("caseNoteType", JoinType.INNER)
      predicateBuilder.add(cb.equal(caseNoteType.get<Any>("sensitive"), false))
    }
    startDate?.let {
      predicateBuilder.add(cb.greaterThanOrEqualTo(root.get("occurrenceDateTime"), startDate))
    }
    endDate?.let {
      predicateBuilder.add(cb.lessThanOrEqualTo(root.get("occurrenceDateTime"), endDate))
    }
    if (typeSubTypes.isNotEmpty()) {
      predicateBuilder.add(getTypesPredicate(root, cb))
    }

    root.fetch<Any, Any>("amendments", JoinType.LEFT)
    val type = root.fetch<Any, Any>("caseNoteType", JoinType.INNER)
    type.fetch<Any, Any>("parentType", JoinType.INNER)
    return cb.and(*predicateBuilder.toTypedArray())
  }

  private fun getTypesPredicate(root: Root<OffenderCaseNote>, cb: CriteriaBuilder): Predicate {
    val typesPredicates: List<Predicate> = typeSubTypes.entries
      .map { (key, value) ->
        if (value.isEmpty()) {
          getTypePredicate(root, cb, key)
        } else {
          getSubtypesPredicate(root, cb, key, value)
        }
      }

    // if we only have one entry then just return that, which prevents an or clause with only one entry
    if (typesPredicates.size == 1) return typesPredicates[0]

    return cb.or(*typesPredicates.toTypedArray<Predicate>())
  }

  private fun getTypePredicate(root: Root<OffenderCaseNote>, cb: CriteriaBuilder, type: String?): Predicate {
    val caseNoteType: Join<Any, Any> = root.join("caseNoteType", JoinType.INNER)
    val parentType: Join<Any, Any> = caseNoteType.join("parentType", JoinType.INNER)
    return cb.equal(parentType.get<Any>("type"), type)
  }

  private fun getSubtypesPredicate(
    root: Root<OffenderCaseNote>,
    cb: CriteriaBuilder,
    type: String,
    subTypes: Set<String>,
  ): Predicate {
    val typePredicateBuilder: ImmutableList.Builder<Predicate> = ImmutableList.builder()

    typePredicateBuilder.add(getTypePredicate(root, cb, type))

    val caseNoteType: Join<Any, Any> = root.join("caseNoteType", JoinType.INNER)
    val inTypes: CriteriaBuilder.In<Any> = cb.`in`(caseNoteType.get("type"))
    subTypes.forEach(Consumer { t: String -> inTypes.value(t) })
    typePredicateBuilder.add(inTypes)

    val typePredicates = typePredicateBuilder.build()
    return cb.and(*typePredicates.toTypedArray<Predicate>())
  }
}
