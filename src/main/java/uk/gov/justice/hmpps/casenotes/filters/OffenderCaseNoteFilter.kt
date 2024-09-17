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
  internal val personIdentifier: String? = null,
  internal val locationId: String? = null,
  internal val authorUsername: String? = null,
  internal val excludeSensitive: Boolean = false,
  internal val startDate: LocalDateTime? = null,
  internal val endDate: LocalDateTime? = null,
  internal val typeSubTypes: Map<String, Set<String>> = emptyMap(),
) : Specification<OffenderCaseNote> {

  override fun toPredicate(root: Root<OffenderCaseNote>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate? {
    val predicateBuilder = mutableListOf<Predicate>()

    if (!personIdentifier.isNullOrBlank()) {
      predicateBuilder.add(cb.equal(root.get<Any>("personIdentifier"), personIdentifier))
    }
    if (!locationId.isNullOrBlank()) {
      predicateBuilder.add(cb.equal(root.get<Any>("locationId"), locationId))
    }
    if (!authorUsername.isNullOrBlank()) {
      predicateBuilder.add(cb.equal(root.get<Any>("authorUsername"), authorUsername))
    }
    if (excludeSensitive) {
      val caseNoteType: Join<Any, Any> = root.join("subType", JoinType.INNER)
      predicateBuilder.add(cb.equal(caseNoteType.get<Any>("sensitive"), false))
    }
    startDate?.let {
      predicateBuilder.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), startDate))
    }
    endDate?.let {
      predicateBuilder.add(cb.lessThanOrEqualTo(root.get("occurredAt"), endDate))
    }
    if (typeSubTypes.isNotEmpty()) {
      predicateBuilder.add(getTypesPredicate(root, cb))
    }

    root.fetch<Any, Any>("amendments", JoinType.LEFT)
    val type = root.fetch<Any, Any>("subType", JoinType.INNER)
    type.fetch<Any, Any>("type", JoinType.INNER)
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

  private fun getTypePredicate(root: Root<OffenderCaseNote>, cb: CriteriaBuilder, typeCode: String?): Predicate {
    val subType: Join<Any, Any> = root.join("subType", JoinType.INNER)
    val type: Join<Any, Any> = subType.join("type", JoinType.INNER)
    return cb.equal(type.get<Any>("code"), typeCode)
  }

  private fun getSubtypesPredicate(
    root: Root<OffenderCaseNote>,
    cb: CriteriaBuilder,
    typeCode: String,
    subTypeCodes: Set<String>,
  ): Predicate {
    val typePredicateBuilder: ImmutableList.Builder<Predicate> = ImmutableList.builder()

    typePredicateBuilder.add(getTypePredicate(root, cb, typeCode))

    val caseNoteType: Join<Any, Any> = root.join("subType", JoinType.INNER)
    val inCodes: CriteriaBuilder.In<Any> = cb.`in`(caseNoteType.get("code"))
    subTypeCodes.forEach(Consumer { t: String -> inCodes.value(t) })
    typePredicateBuilder.add(inCodes)

    val typePredicates = typePredicateBuilder.build()
    return cb.and(*typePredicates.toTypedArray<Predicate>())
  }
}
