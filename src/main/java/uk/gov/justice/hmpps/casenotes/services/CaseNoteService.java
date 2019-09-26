package uk.gov.justice.hmpps.casenotes.services;

import com.google.common.collect.Iterables;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;
import uk.gov.justice.hmpps.casenotes.dto.*;
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType;
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType;
import uk.gov.justice.hmpps.casenotes.repository.CaseNoteTypeRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;
import uk.gov.justice.hmpps.casenotes.repository.ParentCaseNoteTypeRepository;

import javax.persistence.EntityExistsException;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.springframework.data.domain.Sort.Direction.ASC;

@Service
@Transactional(readOnly = true)
@Validated
@AllArgsConstructor
public class CaseNoteService {

    private final OffenderCaseNoteRepository repository;
    private final CaseNoteTypeRepository caseNoteTypeRepository;
    private final ParentCaseNoteTypeRepository parentCaseNoteTypeRepository;
    private final SecurityUserContext securityUserContext;
    private final ExternalApiService externalApiService;
    private final CaseNoteTypeMerger caseNoteTypeMerger;

    public Page<CaseNote> getCaseNotes(final String offenderIdentifier, final CaseNoteFilter caseNoteFilter, final Pageable pageable) {

        final List<CaseNote> sensitiveCaseNotes;

        if (securityUserContext.isOverrideRole("POM", "VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")) {

            final var filter = OffenderCaseNoteFilter.builder()
                    .offenderIdentifier(offenderIdentifier)
                    .type(caseNoteFilter.getType())
                    .subType(caseNoteFilter.getSubType())
                    .locationId(caseNoteFilter.getLocationId())
                    .authorUsername(caseNoteFilter.getAuthorUsername())
                    .startDate(caseNoteFilter.getStartDate())
                    .endDate(caseNoteFilter.getEndDate())
                    .build();

            sensitiveCaseNotes = repository.findAll(filter)
                    .stream()
                    .map(this::mapper)
                    .collect(Collectors.toList());
        } else {
            sensitiveCaseNotes = List.of();
        }

        // only supports one field sort.
        final var direction = pageable.getSort().isSorted() ? pageable.getSort().get().map(Sort.Order::getDirection).collect(Collectors.toList()).get(0) : Sort.Direction.DESC;
        final var sortField = pageable.getSort().isSorted() ? pageable.getSort().get().map(Sort.Order::getProperty).collect(Collectors.toList()).get(0) : "occurrenceDateTime";

        final Page<CaseNote> caseNotes;
        if (sensitiveCaseNotes.isEmpty()) {
            // Just delegate to elite2 for data
            final var pagedNotes = externalApiService.getOffenderCaseNotes(offenderIdentifier, caseNoteFilter, pageable.getPageSize(), pageable.getPageNumber(), sortField, direction);

            final var dtoNotes = translateToDto(pagedNotes, offenderIdentifier);
            caseNotes = new PageImpl<>(dtoNotes, pageable, pagedNotes.getTotalElements());

        } else {
            // There are both case note sources.  Combine
            final var pagedNotes = externalApiService.getOffenderCaseNotes(offenderIdentifier, caseNoteFilter, 1000, 0, "caseNoteId", ASC);

            final var dtoNotes = translateToDto(pagedNotes, offenderIdentifier);

            dtoNotes.addAll(sensitiveCaseNotes);

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

    @SuppressWarnings("unchecked")
    private static List<CaseNote> sortByFieldName(final List<CaseNote> list, final String fieldName, final Sort.Direction direction) {
        try {
            final var field = CaseNote.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            return list.stream()
                    .sorted((first, second) -> {
                        try {
                            return direction == ASC ? ((Comparable<Object>) field.get(first)).compareTo(field.get(second))
                                    : ((Comparable<Object>) field.get(second)).compareTo(field.get(first));
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Error", e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (final NoSuchFieldException e) {
            return list;
        }
    }

    private CaseNote mapper(final OffenderCaseNote cn) {
        final var parentType = cn.getSensitiveCaseNoteType().getParentType();
        return CaseNote.builder()
                .caseNoteId(cn.getId().toString())
                .offenderIdentifier(cn.getOffenderIdentifier())
                .occurrenceDateTime(cn.getOccurrenceDateTime())
                .authorUsername(cn.getAuthorUsername())
                .authorName(cn.getAuthorName())
                .type(parentType.getType())
                .typeDescription(parentType.getDescription())
                .subType(cn.getSensitiveCaseNoteType().getType())
                .subTypeDescription(cn.getSensitiveCaseNoteType().getDescription())
                .source("OCNS") // Indicates its a Offender Case Note Service Type
                .text(cn.getNoteText())
                .creationDateTime(cn.getCreateDateTime())
                .amendments(cn.getAmendments().stream().map(
                        a -> CaseNoteAmendment.builder()
                                .authorUserName(a.getAuthorUsername())
                                .authorName(a.getAuthorName())
                                .additionalNoteText(a.getNoteText())
                                .caseNoteAmendmentId(a.getId())
                                .sequence(a.getAmendSequence())
                                .creationDateTime(a.getCreateDateTime())
                                .build()
                ).collect(Collectors.toList()))
                .locationId(cn.getLocationId())
                .build();
    }

    private CaseNote mapper(final NomisCaseNote cn, final String offenderIdentifier) {
        return CaseNote.builder()
                .caseNoteId(cn.getCaseNoteId().toString())
                .offenderIdentifier(offenderIdentifier)
                .occurrenceDateTime(cn.getOccurrenceDateTime())
                .authorName(cn.getAuthorName())
                .authorUsername(String.valueOf(cn.getStaffId()))  //TODO: this should be username not id.
                .type(cn.getType())
                .typeDescription(cn.getTypeDescription())
                .subType(cn.getSubType())
                .subTypeDescription(cn.getSubTypeDescription())
                .source(cn.getSource())
                .text(cn.getOriginalNoteText())
                .creationDateTime(cn.getCreationDateTime())
                .amendments(cn.getAmendments().stream().map(
                        a -> CaseNoteAmendment.builder()
                                .authorName(a.getAuthorName()) //TODO: Missing the username
                                .additionalNoteText(a.getAdditionalNoteText())
                                .creationDateTime(a.getCreationDateTime())
                                .build()
                ).collect(Collectors.toList()))
                .locationId(cn.getAgencyId())
                .build();
    }

    @Transactional
    public CaseNote createCaseNote(@NotNull final String offenderIdentifier, @NotNull @Valid final NewCaseNote newCaseNote) {
        final var type = caseNoteTypeRepository.findSensitiveCaseNoteTypeByParentType_TypeAndType(newCaseNote.getType(), newCaseNote.getSubType());

        // If we don't have the type locally then won't be secure, so delegate to elite2
        if (type == null) {
            return mapper(externalApiService.createCaseNote(offenderIdentifier, newCaseNote), offenderIdentifier);
        }

        // ensure that the user can then create a secure case note
        if (!securityUserContext.isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES")) {
            throw new AccessDeniedException("User not allowed to create sensitive case notes");
        }

        // ensure that the case note type is active
        if (!type.getParentType().isActive() || !type.isActive()) {
            throw new ValidationException(format("Case Note Type %s/%s is not active", type.getParentType().getType(), type.getType()));
        }

        final var currentUsername = securityUserContext.getCurrentUsername();
        final var staffName = externalApiService.getUserFullName(currentUsername);

        final var locationId = newCaseNote.getLocationId() == null ? externalApiService.getOffenderLocation(offenderIdentifier) : newCaseNote.getLocationId();

        final var caseNote = OffenderCaseNote.builder()
                .noteText(newCaseNote.getText())
                .authorUsername(currentUsername)
                .authorName(staffName)
                .occurrenceDateTime(newCaseNote.getOccurrenceDateTime() == null ? LocalDateTime.now() : newCaseNote.getOccurrenceDateTime())
                .sensitiveCaseNoteType(type)
                .offenderIdentifier(offenderIdentifier)
                .locationId(locationId)
                .build();

        return mapper(repository.save(caseNote));
    }

    @Transactional
    public CaseNote amendCaseNote(@NotNull final String offenderIdentifier, @NotNull final String caseNoteIdentifier, @NotNull @Valid final UpdateCaseNote amendCaseNote) {
        if (isNotSensitiveCaseNote(caseNoteIdentifier)) {
            return mapper(externalApiService.amendOffenderCaseNote(offenderIdentifier, NumberUtils.toLong(caseNoteIdentifier), amendCaseNote), offenderIdentifier);
        }
        if (!securityUserContext.isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES")) {
            throw new AccessDeniedException("User not allowed to view sensitive case notes");
        }

        final var offenderCaseNote = repository.findById(UUID.fromString(caseNoteIdentifier)).orElseThrow(() -> EntityNotFoundException.withId(caseNoteIdentifier));
        if (!offenderIdentifier.equals(offenderCaseNote.getOffenderIdentifier())) {
            throw EntityNotFoundException.withId(offenderIdentifier);
        }

        offenderCaseNote.addAmendment(amendCaseNote.getText(), securityUserContext.getCurrentUsername(), externalApiService.getUserFullName(securityUserContext.getCurrentUsername()));
        repository.save(offenderCaseNote);
        return mapper(offenderCaseNote);
    }

    public List<CaseNoteType> getCaseNoteTypes() {
        final var caseNoteTypes = externalApiService.getCaseNoteTypes();

        if (securityUserContext.isOverrideRole("POM", "VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")) {
            return caseNoteTypeMerger.mergeAndSortList(caseNoteTypes, getSensitiveCaseNoteTypes(true));
        }

        return caseNoteTypes;
    }

    public List<CaseNoteType> getUserCaseNoteTypes() {
        final var userCaseNoteTypes = externalApiService.getUserCaseNoteTypes();
        if (securityUserContext.isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES")) {
            return caseNoteTypeMerger.mergeAndSortList(userCaseNoteTypes, getSensitiveCaseNoteTypes(false));
        }
        return userCaseNoteTypes;
    }


    private List<CaseNoteType> getSensitiveCaseNoteTypes(final boolean allTypes) {
        return parentCaseNoteTypeRepository.findAll().stream()
                .filter(t -> allTypes || t.isActive())
                .map(st -> transform(st, allTypes))
                .collect(Collectors.toList());
    }

    private CaseNoteType transform(final ParentNoteType parentNoteType, final boolean allTypes) {
        return CaseNoteType.builder()
                .code(parentNoteType.getType())
                .description(parentNoteType.getDescription())
                .activeFlag(parentNoteType.isActive() ? "Y" : "N")
                .subCodes(parentNoteType.getSubTypes().stream()
                        .filter(t -> allTypes || t.isActive())
                        .map(st -> CaseNoteType.builder()
                                .code(st.getType())
                                .description(st.getDescription())
                                .activeFlag(st.isActive() ? "Y" : "N")
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public CaseNote getCaseNote(final String offenderIdentifier, final String caseNoteIdentifier) {
        if (isNotSensitiveCaseNote(caseNoteIdentifier)) {
            return mapper(externalApiService.getOffenderCaseNote(offenderIdentifier, NumberUtils.toLong(caseNoteIdentifier)), offenderIdentifier);
        }
        if (!securityUserContext.isOverrideRole("POM", "VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")) {
            throw new AccessDeniedException("User not allowed to view sensitive case notes");
        }
        return mapper(repository.findById(UUID.fromString(caseNoteIdentifier)).orElseThrow(() -> EntityNotFoundException.withId(caseNoteIdentifier)));
    }

    private boolean isNotSensitiveCaseNote(final String caseNoteIdentifier) {
        return NumberUtils.isDigits(caseNoteIdentifier);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MAINTAIN_REF_DATA', 'SYSTEM_USER')")
    public CaseNoteType createCaseNoteType(@NotNull @Valid final NewCaseNoteType newCaseNoteType) {

        final var parentNoteTypeOptional = parentCaseNoteTypeRepository.findById(newCaseNoteType.getType());

        if (parentNoteTypeOptional.isPresent()) {
            throw new EntityExistsException(newCaseNoteType.getType());
        }

        final var parentNoteType = parentCaseNoteTypeRepository.save(ParentNoteType.builder()
                .type(newCaseNoteType.getType())
                .description(newCaseNoteType.getDescription())
                .active(newCaseNoteType.isActive())
                .build());

        return transform(parentNoteType, true);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MAINTAIN_REF_DATA', 'SYSTEM_USER')")
    public CaseNoteType createCaseNoteSubType(final String parentType, @NotNull @Valid final NewCaseNoteType newCaseNoteType) {
        final var parentNoteType = parentCaseNoteTypeRepository.findById(parentType).orElseThrow(EntityNotFoundException.withId(parentType));

        if (parentNoteType.getSubType(newCaseNoteType.getType()).isPresent()) {
            throw new EntityExistsException(newCaseNoteType.getType());
        }
        parentNoteType.getSubTypes().add(
                SensitiveCaseNoteType.builder()
                        .type(newCaseNoteType.getType())
                        .description(newCaseNoteType.getDescription())
                        .active(newCaseNoteType.isActive())
                        .parentType(parentNoteType)
                        .build()
        );

        return transform(parentNoteType, true);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MAINTAIN_REF_DATA', 'SYSTEM_USER')")
    public CaseNoteType updateCaseNoteType(final String parentType, @NotNull @Valid final UpdateCaseNoteType body) {
        final var parentNoteType = parentCaseNoteTypeRepository.findById(parentType).orElseThrow(EntityNotFoundException.withId(parentType));
        parentNoteType.update(body.getDescription(), body.isActive());
        return transform(parentNoteType, true);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MAINTAIN_REF_DATA', 'SYSTEM_USER')")
    public CaseNoteType updateCaseNoteSubType(final String parentType, final String subType, @NotNull @Valid final UpdateCaseNoteType body) {

        final var parentNoteType = parentCaseNoteTypeRepository.findById(parentType).orElseThrow(EntityNotFoundException.withId(parentType));
        final var existingSubType = parentNoteType.getSubType(subType).orElseThrow(EntityNotFoundException.withId(parentType + " " + subType));
        existingSubType.update(body.getDescription(), body.isActive());
        return transform(parentNoteType, true);
    }

    @PreAuthorize("hasAnyRole('CASE_NOTE_EVENTS', 'SYSTEM_USER')")
    public CaseNoteEvents getCaseNoteEvents(@NotEmpty final List<String> noteTypes, @NotNull final LocalDateTime createdDate, @Min(1) @Max(5000) final int limit) {
        final var eliteDate = LocalDateTime.now();
        final var eliteEvents = externalApiService.getCaseNoteEvents(noteTypes, createdDate, limit);
        return combineWithSensitive(noteTypes, createdDate, limit, eliteDate, eliteEvents);
    }

    @PreAuthorize("hasAnyRole('CASE_NOTE_EVENTS', 'SYSTEM_USER')")
    public CaseNoteEvents getCaseNoteEvents(@NotEmpty final List<String> noteTypes, @NotNull final LocalDateTime createdDate) {
        final var eliteDate = LocalDateTime.now();
        final var eliteEvents = externalApiService.getCaseNoteEvents(noteTypes, createdDate);
        return combineWithSensitive(noteTypes, createdDate, Integer.MAX_VALUE, eliteDate, eliteEvents);
    }

    private CaseNoteEvents combineWithSensitive(final List<String> noteTypes, final LocalDateTime createdDate, final int limit, final LocalDateTime eliteDate, final List<CaseNoteEvent> eliteEvents) {
        final var noteTypesMap = splitTypesAndSubTypes(noteTypes);

        final var sensitiveNotes = repository.findBySensitiveCaseNoteType_ParentType_TypeInAndModifyDateTimeAfterOrderByModifyDateTime(noteTypesMap.keySet(), createdDate, PageRequest.of(0, limit));
        final var sensitiveEvents = sensitiveNotes.stream().filter((event) -> {
            final var subTypes = noteTypesMap.get(event.getSensitiveCaseNoteType().getParentType().getType());
            // will be null if not in map, otherwise will be empty if type in map with no sub type set
            return subTypes != null && (subTypes.isEmpty() || subTypes.contains(event.getSensitiveCaseNoteType().getType()));
        }).map(CaseNoteEvent::new).collect(Collectors.toList());

        final var events = Stream.of(eliteEvents, sensitiveEvents)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(CaseNoteEvent::getNotificationTimestamp))
                .limit(limit)
                .collect(Collectors.toList());

        // if we've limited the results then grab earliest entry from the events
        final var lastDate = events.isEmpty() || events.size() != limit ? eliteDate : Iterables.getLast(events).getNotificationTimestamp();
        // also earliest we can possibly return is when we called elite2 (since that was called first)
        return new CaseNoteEvents(events, eliteDate.isAfter(lastDate) ? lastDate : eliteDate);
    }

    private Map<String, List<?>> splitTypesAndSubTypes(final List<String> noteTypes) {
        return noteTypes.stream()
                .map(t -> t.trim().replace(' ', '+'))
                .collect(Collectors.toMap(
                        (n) -> StringUtils.substringBefore(n, "+"),
                        (n) -> {
                            final var subType = StringUtils.substringAfter(n, "+");
                            return subType.isEmpty() ? List.of() : List.of(subType);
                        },
                        (v1, v2) -> Stream.of(v1, v2).flatMap(Collection::stream).collect(Collectors.toList())));
    }
}
