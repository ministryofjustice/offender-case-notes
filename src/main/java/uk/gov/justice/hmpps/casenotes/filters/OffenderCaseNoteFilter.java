package uk.gov.justice.hmpps.casenotes.filters;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;

@Builder
@EqualsAndHashCode
public class OffenderCaseNoteFilter implements Specification<OffenderCaseNote> {

    private final String offenderIdentifier;
    private final String locationId;
    private final String authorUsername;
    private final String type;
    private final String subType;
    private final boolean excludeSensitive;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;

    @Override
    public Predicate toPredicate(final Root<OffenderCaseNote> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        final ImmutableList.Builder<Predicate> predicateBuilder = ImmutableList.builder();

        if (StringUtils.isNotBlank(offenderIdentifier)) {
            predicateBuilder.add(cb.equal(root.get("offenderIdentifier"), offenderIdentifier));
        }
        if (StringUtils.isNotBlank(locationId)) {
            predicateBuilder.add(cb.equal(root.get("locationId"), locationId));
        }
        if (StringUtils.isNotBlank(authorUsername)) {
            predicateBuilder.add(cb.equal(root.get("authorUsername"), authorUsername));
        }
        if (StringUtils.isNotBlank(type)) {
            final var caseNoteType = root.join("caseNoteType", JoinType.INNER);
            final var parentType = caseNoteType.join("parentType", JoinType.INNER);
            predicateBuilder.add(cb.equal(parentType.get("type"), type));
        }
        if (StringUtils.isNotBlank(subType)) {
            final var caseNoteType = root.join("caseNoteType", JoinType.INNER);
            predicateBuilder.add(cb.equal(caseNoteType.get("type"), subType));
        }
        if (excludeSensitive) {
            final var caseNoteType = root.join("caseNoteType", JoinType.INNER);
            predicateBuilder.add(cb.equal(caseNoteType.get("sensitive"), false));
        }
        if (startDate != null) {
            predicateBuilder.add(cb.greaterThanOrEqualTo(root.get("occurrenceDateTime"), startDate));
        }
        if (endDate != null) {
            predicateBuilder.add(cb.lessThanOrEqualTo(root.get("occurrenceDateTime"), endDate));
        }

        final var predicates = predicateBuilder.build();
        root.fetch("amendments", JoinType.LEFT);
        return cb.and(predicates.toArray(new Predicate[0]));
    }

}

