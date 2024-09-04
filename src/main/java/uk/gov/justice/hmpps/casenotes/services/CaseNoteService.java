package uk.gov.justice.hmpps.casenotes.services;

import com.microsoft.applicationinsights.TelemetryClient;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote;
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.notes.AmendCaseNoteRequest;
import uk.gov.justice.hmpps.casenotes.notes.CaseNote;
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest;
import uk.gov.justice.hmpps.casenotes.repository.CaseNoteTypeRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteAmendmentRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Comparator.comparing;

@Service
@Transactional(readOnly = true)
@Validated
@AllArgsConstructor
@Slf4j
public class CaseNoteService {

    private static final String SERVICE_NAME = "OCNS";
    private final OffenderCaseNoteRepository repository;
    private final OffenderCaseNoteAmendmentRepository amendmentRepository;
    private final CaseNoteTypeRepository caseNoteTypeRepository;
    private final SecurityUserContext securityUserContext;
    private final ExternalApiService externalApiService;
    private final TelemetryClient telemetryClient;
    private final EntityManager entityManager;

    public Page<CaseNote> getCaseNotes(
        final String offenderIdentifier,
        final CaseNoteFilter caseNoteFilter,
        final Pageable pageable
    ) {

        final List<CaseNote> sensitiveCaseNotes;

        final var includeSensitiveCaseNotes =
            isAllowedToViewOrCreateSensitiveCaseNote() && caseNoteFilter.getIncludeSensitive();
        final var filter = new OffenderCaseNoteFilter(
            offenderIdentifier,
            caseNoteFilter.getLocationId(),
            caseNoteFilter.getAuthorUsername(),
            !includeSensitiveCaseNotes,
            caseNoteFilter.getStartDate(),
            caseNoteFilter.getEndDate(),
            caseNoteFilter.getTypesAndSubTypes()
        );

        sensitiveCaseNotes = repository.findAll(filter)
            .stream()
            .map(this::mapper)
            .toList();

        final Page<CaseNote> caseNotes;
        if (sensitiveCaseNotes.isEmpty()) {
            // Just delegate to prison api for data
            final var pagedNotes =
                externalApiService.getOffenderCaseNotes(offenderIdentifier, caseNoteFilter, pageable);

            final var dtoNotes = translateToDto(pagedNotes, offenderIdentifier);
            caseNotes = new PageImpl<>(dtoNotes, pageable, pagedNotes.getTotalElements());

        } else {
            // There are both case note sources.  Combine
            final var pagedNotes =
                externalApiService.getOffenderCaseNotes(offenderIdentifier, caseNoteFilter, PageRequest.of(0, 10000));

            final var dtoNotes = translateToDto(pagedNotes, offenderIdentifier);

            dtoNotes.addAll(sensitiveCaseNotes);

            // only supports one field sort.
            final var direction = pageable.getSort().isSorted() ? pageable.getSort().get().map(Sort.Order::getDirection)
                .toList().getFirst() : Direction.DESC;
            final var sortField = pageable.getSort().isSorted() ? pageable.getSort().get().map(Sort.Order::getProperty)
                .toList().getFirst() : "occurrenceDateTime";

            final var sortedList = sortByFieldName(dtoNotes, sortField, direction);

            final var toIndex = (int) (pageable.getOffset() + pageable.getPageSize());
            final var pagedList = sortedList.subList((int) pageable.getOffset(), Math.min(toIndex, sortedList.size()));

            caseNotes = new PageImpl<>(pagedList, pageable, pagedNotes.getTotalElements() + sensitiveCaseNotes.size());
        }
        return caseNotes;
    }

    private List<CaseNote> translateToDto(final Page<NomisCaseNote> pagedNotes, final String offenderIdentifier) {
        return pagedNotes.getContent()
            .stream()
            .map(cn -> mapper(cn, offenderIdentifier))
            .collect(Collectors.toList());
    }

    private static List<CaseNote> sortByFieldName(
        final List<CaseNote> list,
        final String fieldName,
        final Sort.Direction direction
    ) {
        final Comparator<CaseNote> compare = fieldName.equalsIgnoreCase("creationDateTime") ?
            comparing(CaseNote::getCreationDateTime) : comparing(CaseNote::getOccurrenceDateTime);

        final var sort = direction == Direction.ASC ? compare : compare.reversed();

        return list.stream().sorted(sort).toList();
    }

    private CaseNote mapper(final OffenderCaseNote cn) {
        final var parentType = cn.getCaseNoteType().getParentType();
        return CaseNote.builder()
            .caseNoteId(cn.getId().toString())
            .eventId(cn.getEventId())
            .offenderIdentifier(cn.getOffenderIdentifier())
            .occurrenceDateTime(cn.getOccurrenceDateTime())
            .authorUserId(cn.getAuthorUserId())
            .authorName(cn.getAuthorName())
            .type(parentType.getType())
            .typeDescription(parentType.getDescription())
            .subType(cn.getCaseNoteType().getType())
            .subTypeDescription(cn.getCaseNoteType().getDescription())
            .source(SERVICE_NAME) // Indicates its a Offender Case Note Service Type
            .sensitive(cn.getCaseNoteType().isSensitive())
            .text(cn.getNoteText())
            .creationDateTime(cn.getCreateDateTime())
            .systemGenerated(cn.isSystemGenerated())
            .legacyId(cn.getLegacyId())
            .amendments(cn.getAmendments().stream().map(
                a -> new CaseNoteAmendment(
                    a.getCreateDateTime(),
                    a.getAuthorUsername(),
                    a.getAuthorName(),
                    a.getAuthorUserId(),
                    a.getNoteText()
                )
            ).toList())
            .locationId(cn.getLocationId())
            .build();
    }

    private CaseNote mapper(final NomisCaseNote cn, final String offenderIdentifier) {
        return CaseNote.builder()
            .caseNoteId(cn.getCaseNoteId().toString())
            .eventId(cn.getCaseNoteId())
            .offenderIdentifier(offenderIdentifier)
            .occurrenceDateTime(cn.getOccurrenceDateTime())
            .authorName(cn.getAuthorName())
            .authorUserId(valueOf(cn.getStaffId()))
            .type(cn.getType())
            .typeDescription(cn.getTypeDescription())
            .subType(cn.getSubType())
            .subTypeDescription(cn.getSubTypeDescription())
            .source(cn.getSource())
            .sensitive(false)
            .text(cn.getOriginalNoteText())
            .creationDateTime(cn.getCreationDateTime())
            .systemGenerated(cn.getSource().equalsIgnoreCase("AUTO"))
            .legacyId(cn.getCaseNoteId())
            .amendments(cn.getAmendments().stream().map(
                a -> new CaseNoteAmendment(
                    a.getCreationDateTime(),
                    a.getAuthorUsername(),
                    a.getAuthorName(),
                    a.getAuthorUserId(),
                    a.getAdditionalNoteText()
                )
            ).toList())
            .locationId(cn.getAgencyId())
            .build();
    }

    @Transactional
    public CaseNote createCaseNote(
        @NotNull final String offenderIdentifier,
        @NotNull @Valid final CreateCaseNoteRequest newCaseNote
    ) {
        final var type = caseNoteTypeRepository.findByParentTypeAndType(
            newCaseNote.getType(), newCaseNote.getSubType()
        ).orElseThrow(() ->
            new IllegalArgumentException(format(
                "Unknown Case Note Type %s/%s",
                newCaseNote.getType(),
                newCaseNote.getSubType()
            ))
        );

        // If we don't have the type locally then won't be secure, so delegate to prison-api
        if (type.isSyncToNomis()) {
            if (newCaseNote.getSystemGenerated() == null) {
                newCaseNote.setSystemGenerated(!type.isDpsUserSelectable());
            }
            return mapper(externalApiService.createCaseNote(offenderIdentifier, newCaseNote), offenderIdentifier);
        }

        // ensure that the user can then create the case note
        if (type.isRestrictedUse() && !isAllowedToCreateRestrictedCaseNote()) {
            throw new AccessDeniedException("User not allowed to create this case note type [" + type + "]");
        }

        // ensure that the case note type is active
        if (!type.isActive()) {
            throw new ValidationException(format(
                "Case Note Type %s/%s is not active",
                type.getParentType().getType(),
                type.getType()
            ));
        }

        final CaseNoteRequestContext context = CaseNoteRequestContext.Companion.get();

        final var locationId =
            newCaseNote.getLocationId() == null ? externalApiService.getOffenderLocation(offenderIdentifier) : newCaseNote.getLocationId();

        final var caseNote = OffenderCaseNote.builder()
            .noteText(newCaseNote.getText())
            .authorUsername(context.getUsername())
            .authorUserId(context.getUserId())
            .authorName(context.getUserDisplayName())
            .occurrenceDateTime(newCaseNote.getOccurrenceDateTime())
            .caseNoteType(type)
            .offenderIdentifier(offenderIdentifier)
            .locationId(locationId)
            .systemGenerated(TRUE.equals(newCaseNote.getSystemGenerated()))
            .build();

        // save and flush to activate database event id generation
        var saved = repository.saveAndFlush(caseNote);
        // refresh so the entity is populated with the event id before attempting to map
        entityManager.refresh(saved);
        return mapper(saved);
    }

    @Transactional
    public CaseNote amendCaseNote(
        @NotNull final String offenderIdentifier,
        @NotNull final String caseNoteIdentifier,
        @NotNull @Valid final AmendCaseNoteRequest amendCaseNoteRequest
    ) {
        final boolean isLegacy = isLegacyId(caseNoteIdentifier);
        final var offenderCaseNote = getCaseNote(caseNoteIdentifier, isLegacy);
        if (isLegacy) {
            return mapper(externalApiService.amendOffenderCaseNote(
                offenderIdentifier,
                NumberUtils.toLong(caseNoteIdentifier),
                amendCaseNoteRequest
            ), offenderIdentifier);
        } else {
            if (offenderCaseNote.getCaseNoteType().isRestrictedUse() && !isAllowedToCreateRestrictedCaseNote()) {
                throw new AccessDeniedException("User not allowed to amend this case note type [" + offenderCaseNote.getCaseNoteType() + "]");
            }

            if (!offenderIdentifier.equals(offenderCaseNote.getOffenderIdentifier())) {
                throw EntityNotFoundException.withId(offenderIdentifier);
            }

            final var context = CaseNoteRequestContext.Companion.get();
            offenderCaseNote.addAmendment(
                amendCaseNoteRequest.getText(),
                context.getUsername(),
                context.getUserDisplayName(),
                context.getUserId()
            );
            return mapper(repository.save(offenderCaseNote));
        }
    }

    private OffenderCaseNote getCaseNote(final String caseNoteIdentifier, boolean isLegacy) {
        return isLegacy ? null : repository.findById(UUID.fromString(caseNoteIdentifier))
            .orElseThrow(() -> EntityNotFoundException.withId(caseNoteIdentifier));
    }

    public CaseNote getCaseNote(final String offenderIdentifier, final String caseNoteIdentifier) {
        final boolean isLegacy = isLegacyId(caseNoteIdentifier);
        final OffenderCaseNote caseNote = getCaseNote(caseNoteIdentifier, isLegacy);
        if (isLegacy) {
            return mapper(externalApiService.getOffenderCaseNote(
                offenderIdentifier,
                NumberUtils.toLong(caseNoteIdentifier)
            ), offenderIdentifier);
        }

        if (caseNote.getCaseNoteType().isSensitive() && !isAllowedToViewOrCreateSensitiveCaseNote()) {
            throw new AccessDeniedException("User not allowed to view sensitive case notes");
        }
        return mapper(caseNote);
    }

    private boolean isLegacyId(final String caseNoteIdentifier) {
        return NumberUtils.isDigits(caseNoteIdentifier);
    }

    @Transactional
    public int deleteCaseNotesForOffender(final String offenderIdentifier) {
        repository.deleteOffenderCaseNoteAmendmentsByOffenderIdentifier(offenderIdentifier);
        final var deletedCaseNotesCount = repository.deleteOffenderCaseNoteByOffenderIdentifier(offenderIdentifier);
        log.info("Deleted {} case notes for offender identifier {}", deletedCaseNotesCount, offenderIdentifier);
        telemetryClient.trackEvent(
            "OffenderDelete",
            Map.of("offenderNo", offenderIdentifier, "count", valueOf(deletedCaseNotesCount)),
            null
        );
        return deletedCaseNotesCount;
    }

    @Transactional
    @PreAuthorize("hasRole('DELETE_SENSITIVE_CASE_NOTES')")
    public void softDeleteCaseNote(final String offenderIdentifier, final String caseNoteId) {
        if (isLegacyId(caseNoteId)) {
            throw new ValidationException("Case note id not a sensitive case note, please delete through NOMIS");
        }
        final var caseNote = repository.findById(UUID.fromString(caseNoteId))
            .orElseThrow(() -> new EntityNotFoundException("Case note not found"));
        if (!caseNote.getOffenderIdentifier().equalsIgnoreCase(offenderIdentifier)) {
            throw new ValidationException("case note id not connected with offenderIdentifier");
        }
        repository.deleteById(UUID.fromString(caseNoteId));
        telemetryClient.trackEvent(
            "SecureCaseNoteSoftDelete",
            Map.of("userName", securityUserContext.getCurrentUser().getUsername(),
                "offenderId", offenderIdentifier,
                "case note id", caseNoteId
            ),
            null
        );
    }

    private boolean isAllowedToCreateRestrictedCaseNote() {
        return securityUserContext.isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES");
    }

    private boolean isAllowedToViewOrCreateSensitiveCaseNote() {
        return securityUserContext.isOverrideRole("POM", "VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES");
    }
}
