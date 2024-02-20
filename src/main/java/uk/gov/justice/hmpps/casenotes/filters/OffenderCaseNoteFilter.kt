package uk.gov.justice.hmpps.casenotes.filters

import com.google.common.collect.ImmutableList
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.apache.commons.lang3.StringUtils
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import java.time.LocalDateTime
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream

class OffenderCaseNoteFilter(
  private val offenderIdentifier: String? = null,
  private val locationId: String? = null,
  private val authorUsername: String? = null,
  private val excludeSensitive: Boolean = false,
  private val startDate: LocalDateTime? = null,
  private val endDate: LocalDateTime? = null,
  private val typeSubTypes: List<String> = emptyList(),
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
    return cb.and(*predicateBuilder.toTypedArray())
  }

  private fun getTypesPredicate(root: Root<OffenderCaseNote>, cb: CriteriaBuilder): Predicate {
    val typesAndSubTypes: Map<String, List<String>> = splitTypes(typeSubTypes)
    val typesPredicates: List<Predicate> = typesAndSubTypes.entries
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
    subTypes: List<String>,
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

  private fun splitTypes(types: List<String>): Map<String, List<String>> {
    return types.stream()
      .map { t: String -> t.trim().replace(' ', '+') }
      .collect(
        Collectors.toMap(
          { n -> StringUtils.substringBefore(n, "+") },
          { n ->
            val subtype: String = StringUtils.substringAfter(n, "+")
            if (subtype.isEmpty()) listOf() else listOf(subtype)
          },
          { v1, v2 -> Stream.of(v1, v2).flatMap { obj -> obj.stream() }.toList() },
        ),
      )
  }
}
