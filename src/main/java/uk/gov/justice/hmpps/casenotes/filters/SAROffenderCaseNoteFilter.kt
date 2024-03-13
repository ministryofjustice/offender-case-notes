package uk.gov.justice.hmpps.casenotes.filters

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import java.time.LocalDate
import java.time.LocalTime

class SAROffenderCaseNoteFilter(
  private val offenderIdentifier: String? = null,
  private val startDate: LocalDate? = null,
  private val endDate: LocalDate? = null,
) : Specification<OffenderCaseNote> {

  override fun toPredicate(root: Root<OffenderCaseNote>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate? {
    val predicateBuilder = mutableListOf<Predicate>()

    if (!offenderIdentifier.isNullOrBlank()) {
      predicateBuilder.add(cb.equal(root.get<Any>("offenderIdentifier"), offenderIdentifier))
    }

    startDate?.let {
      predicateBuilder.add(cb.greaterThanOrEqualTo(root.get("createDateTime"), startDate.atStartOfDay()))
    }
    endDate?.let {
      predicateBuilder.add(cb.lessThanOrEqualTo(root.get("createDateTime"), endDate.atTime(LocalTime.MAX)))
    }

    root.fetch<Any, Any>("amendments", JoinType.LEFT)
    return cb.and(*predicateBuilder.toTypedArray())
  }
}
