package uk.gov.justice.hmpps.casenotes.filters

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNoteAmendment
import java.time.LocalDate
import java.time.LocalTime

class SAROffenderCaseNoteAmendmentFilter(
  private val pNum: String?,
  private val startDate: LocalDate? = null,
  private val endDate: LocalDate? = null,
) : Specification<OffenderCaseNoteAmendment> {

  override fun toPredicate(root: Root<OffenderCaseNoteAmendment>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate? {
    val predicateBuilder = mutableListOf<Predicate>()

    startDate?.let {
      predicateBuilder.add(cb.greaterThanOrEqualTo(root.get("createDateTime"), startDate.atStartOfDay()))
    }
    endDate?.let {
      predicateBuilder.add(cb.lessThanOrEqualTo(root.get("createDateTime"), endDate.atTime(LocalTime.MAX)))
    }

    val offenderCaseNote = root.fetch<Any, Any>("caseNote", JoinType.LEFT) as Join<*, *>

    cb.equal(offenderCaseNote.get<Any>("offenderIdentifier"), pNum)
    predicateBuilder.add(cb.equal(offenderCaseNote.get<Any>("offenderIdentifier"), pNum))

    return cb.and(*predicateBuilder.toTypedArray())
  }
}
