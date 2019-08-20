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

    private final String offenderIdentifier;
    private final String locationId;
    private final String authorUsername;
    private final String type;
    private final String subType;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;

    @Override
    public Predicate toPredicate(final Root<OffenderCaseNote> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        final ImmutableList.Builder<Predicate> predicateBuilder = ImmutableList.builder();

        if (offenderIdentifier != null) {
            predicateBuilder.add(cb.equal(root.get("offenderIdentifier"), offenderIdentifier));
        }
        if (locationId != null) {
            predicateBuilder.add(cb.equal(root.get("locationId"), locationId));
        }
        if (authorUsername != null) {
            predicateBuilder.add(cb.equal(root.get("authorUsername"), authorUsername));
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

        final var predicates = predicateBuilder.build();
        return cb.and(predicates.toArray(new Predicate[0]));
    }

}

