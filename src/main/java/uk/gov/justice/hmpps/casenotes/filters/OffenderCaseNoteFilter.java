package uk.gov.justice.hmpps.casenotes.filters;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;

@Builder
@EqualsAndHashCode
public class OffenderCaseNoteFilter implements Specification<OffenderCaseNote> {

    private String offenderIdentifier;
    private String locationId;
    private String staffUsername;
    private String type;
    private String subType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Override
    public Predicate toPredicate(Root<OffenderCaseNote> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        ImmutableList.Builder<Predicate> predicateBuilder = ImmutableList.builder();

        if (offenderIdentifier != null) {
            predicateBuilder.add(cb.equal(root.get("offenderIdentifier"), offenderIdentifier));
        }
        if (locationId != null) {
            predicateBuilder.add(cb.equal(root.get("locationId"), locationId));
        }
        if (staffUsername != null) {
            predicateBuilder.add(cb.equal(root.get("authorUsername"), staffUsername));
        }
        if (type != null) {
            final var caseNoteType = root.join("sensitiveCaseNoteType", JoinType.INNER);
            final var parentType = caseNoteType.join("parentType", JoinType.INNER);
            predicateBuilder.add(cb.equal(parentType.get("type"), type));
        }
        if (subType != null) {
            final var caseNoteType = root.join("sensitiveCaseNoteType", JoinType.INNER);
            predicateBuilder.add(cb.equal(caseNoteType.get("type"), subType));
        }
        if (startDate != null) {
            predicateBuilder.add(cb.greaterThanOrEqualTo(root.get("occurrenceDateTime"), startDate));
        }
        if (endDate != null) {
            predicateBuilder.add(cb.lessThanOrEqualTo(root.get("occurrenceDateTime"), endDate));
        }

        ImmutableList<Predicate> predicates = predicateBuilder.build();
        return cb.and(predicates.toArray(new Predicate[0]));
    }

}

