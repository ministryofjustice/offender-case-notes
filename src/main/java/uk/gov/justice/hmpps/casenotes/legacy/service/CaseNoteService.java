package uk.gov.justice.hmpps.casenotes.legacy.service;

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent;
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent.Type;
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.legacy.dto.NomisCaseNote;
import uk.gov.justice.hmpps.casenotes.legacy.filters.OffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.legacy.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.notes.AmendCaseNoteRequest;
import uk.gov.justice.hmpps.casenotes.notes.CaseNote;
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest;
import uk.gov.justice.hmpps.casenotes.legacy.repository.CaseNoteSubTypeRepository;
import uk.gov.justice.hmpps.casenotes.legacy.repository.OffenderCaseNoteRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Comparator.comparing;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

@Service
@Transactional(readOnly = true)
@Validated
@AllArgsConstructor
@Slf4j
public class CaseNoteService {

    private static final String SERVICE_NAME = "OCNS";
    private final OffenderCaseNoteRepository repository;
    private final CaseNoteSubTypeRepository caseNoteSubTypeRepository;
    private final SecurityUserContext securityUserContext;
    private final ExternalApiService externalApiService;
    private final ApplicationEventPublisher eventPublisher;

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
                .toList().getFirst() : "occurredAt";

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
            comparing(CaseNote::getCreatedAt) : comparing(CaseNote::getOccurredAt);

        final var sort = direction == Direction.ASC ? compare : compare.reversed();

        return list.stream().sorted(sort).toList();
    }

    private CaseNote mapper(final OffenderCaseNote cn) {
        final var parentType = cn.getSubType().getType();
        return CaseNote.builder()
            .id(cn.getId().toString())
            .personIdentifier(cn.getPersonIdentifier())
            .occurredAt(cn.getOccurredAt())
            .authorUserId(cn.getAuthorUserId())
            .authorUsername(cn.getAuthorUsername())
            .authorName(cn.getAuthorName())
            .type(parentType.getCode())
            .typeDescription(parentType.getDescription())
            .subType(cn.getSubType().getCode())
            .subTypeDescription(cn.getSubType().getDescription())
            .source(SERVICE_NAME) // Indicates its a Offender Case Note Service Type
            .sensitive(cn.getSubType().isSensitive())
            .text(cn.getText())
            .createdAt(cn.getCreatedAt())
            .systemGenerated(cn.getSystemGenerated())
            .legacyId(cn.getLegacyId())
            .eventId(cn.getLegacyId())
            .amendments(cn.getAmendments().stream().map(
                a -> new CaseNoteAmendment(
                    a.getCreatedAt(),
                    a.getAuthorUsername(),
                    a.getAuthorName(),
                    a.getAuthorUserId(),
                    a.getText(),
                    a.getId()
                )
            ).toList())
            .locationId(cn.getLocationId())
            .build();
    }

    private CaseNote mapper(final NomisCaseNote cn, final String offenderIdentifier) {
        return CaseNote.builder()
            .id(cn.getId().toString())
            .eventId(cn.getId())
            .personIdentifier(offenderIdentifier)
            .occurredAt(cn.getOccurredAt())
            .authorName(cn.getAuthorName())
            .authorUserId(valueOf(cn.getStaffId()))
            .authorUsername(cn.getAuthorUsername())
            .type(cn.getType())
            .typeDescription(cn.getTypeDescription())
            .subType(cn.getSubType())
            .subTypeDescription(cn.getSubTypeDescription())
            .source(cn.getSource())
            .sensitive(false)
            .text(cn.getOriginalNoteText())
            .createdAt(cn.getCreatedAt())
            .systemGenerated(cn.getSource().equalsIgnoreCase("AUTO"))
            .legacyId(cn.getId())
            .amendments(cn.getAmendments().stream().map(
                a -> new CaseNoteAmendment(
                    a.getCreationDateTime(),
                    a.getAuthorUsername(),
                    a.getAuthorName(),
                    a.getAuthorUserId(),
                    a.getAdditionalNoteText(),
                    null
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
        final var type = caseNoteSubTypeRepository.findByParentTypeAndType(
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
                type.getType().getCode(),
                type.getType()
            ));
        }

        final CaseNoteRequestContext context = CaseNoteRequestContext.Companion.get();

        final var caseNote = OffenderCaseNote.builder()
            .text(newCaseNote.getText())
            .authorUsername(context.getUsername())
            .authorUserId(context.getUserId())
            .authorName(context.getUserDisplayName())
            .occurredAt(coalesce(newCaseNote.getOccurrenceDateTime(), context.getRequestAt()))
            .subType(type)
            .personIdentifier(offenderIdentifier)
            .locationId(newCaseNote.getLocationId())
            .systemGenerated(TRUE.equals(newCaseNote.getSystemGenerated()))
            .build();

        // save and flush to activate database event id generation
        var saved = repository.saveAndFlush(caseNote);
        // refresh so the entity is populated with the event id before attempting to map
        repository.refresh(saved);
        eventPublisher.publishEvent(generateEvent(Type.CREATED, saved));
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
            if (offenderCaseNote.getSubType().isRestrictedUse() && !isAllowedToCreateRestrictedCaseNote()) {
                throw new AccessDeniedException("User not allowed to amend this case note type [" + offenderCaseNote.getSubType() + "]");
            }

            if (!offenderIdentifier.equals(offenderCaseNote.getPersonIdentifier())) {
                throw EntityNotFoundException.withId(offenderIdentifier);
            }

            final var context = CaseNoteRequestContext.Companion.get();
            offenderCaseNote.addAmendment(
                amendCaseNoteRequest.getText(),
                context.getUsername(),
                context.getUserDisplayName(),
                context.getUserId()
            );
            eventPublisher.publishEvent(generateEvent(Type.UPDATED, offenderCaseNote));
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

        if (caseNote.getSubType().isSensitive() && !isAllowedToViewOrCreateSensitiveCaseNote()) {
            throw new AccessDeniedException("User not allowed to view sensitive case notes");
        }
        return mapper(caseNote);
    }

    private boolean isLegacyId(final String caseNoteIdentifier) {
        return NumberUtils.isDigits(caseNoteIdentifier);
    }

    private boolean isAllowedToCreateRestrictedCaseNote() {
        return securityUserContext.isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES");
    }

    private boolean isAllowedToViewOrCreateSensitiveCaseNote() {
        return securityUserContext.isOverrideRole("POM", "VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES");
    }

    private PersonCaseNoteEvent generateEvent(PersonCaseNoteEvent.Type type, OffenderCaseNote caseNote) {
     return new PersonCaseNoteEvent(
         type,
         caseNote.getPersonIdentifier(),
         caseNote.getId(),
         caseNote.getLegacyId(),
         caseNote.getSubType().getType().getCode(),
         caseNote.getSubType().getCode(),
         CaseNoteRequestContext.get().getSource(),
         caseNote.getSubType().isSyncToNomis(),
         false,
         null
     );
    }
}
