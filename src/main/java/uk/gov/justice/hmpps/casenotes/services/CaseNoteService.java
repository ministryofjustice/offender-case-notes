package uk.gov.justice.hmpps.casenotes.services;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;
import uk.gov.justice.hmpps.casenotes.dto.*;
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType;
import uk.gov.justice.hmpps.casenotes.repository.CaseNoteTypeRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;
import uk.gov.justice.hmpps.casenotes.repository.ParentCaseNoteTypeRepository;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public Page<CaseNote> getCaseNotes(final String offenderIdentifier, final CaseNoteFilter caseNoteFilter, final Pageable pageable) {

        final var sensitiveCaseNotes = new ArrayList<CaseNote>();

        if (securityUserContext.isOverrideRole("VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")) {

            final var filter = OffenderCaseNoteFilter.builder()
                    .offenderIdentifier(offenderIdentifier)
                    .type(caseNoteFilter.getType())
                    .subType(caseNoteFilter.getSubType())
                    .locationId(caseNoteFilter.getLocationId())
                    .authorUsername(caseNoteFilter.getAuthorUsername())
                    .startDate(caseNoteFilter.getStartDate())
                    .endDate(caseNoteFilter.getEndDate())
                    .build();

            sensitiveCaseNotes.addAll(repository.findAll(filter)
                    .stream()
                    .map(this::mapper)
                    .collect(Collectors.toList()));
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
            final var pagedList = sortedList.subList((int) pageable.getOffset(), toIndex > sortedList.size() ? sortedList.size() : toIndex);

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
                .authorUsername(cn.getStaffUsername())
                .authorName(cn.getStaffName())
                .type(parentType.getType())
                .typeDescription(parentType.getDescription())
                .subType(cn.getSensitiveCaseNoteType().getType())
                .subTypeDescription(cn.getSensitiveCaseNoteType().getDescription())
                .source("OCNS") // Indicates its a Offender Case Note Service Type
                .text(cn.getNoteText())
                .creationDateTime(cn.getCreateDateTime())
                .amendments(cn.getAmendments().stream().map(
                        a -> CaseNoteAmendment.builder()
                                .authorUserName(a.getStaffUsername())
                                .authorName(a.getStaffName())
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
        final var parentNoteTypeOptional = parentCaseNoteTypeRepository.findById(newCaseNote.getType());

        // If we don't have the type locally then won't be secure, so delegate to elite2
        if (parentNoteTypeOptional.isEmpty()) {
            return mapper(externalApiService.createCaseNote(offenderIdentifier, newCaseNote), offenderIdentifier);
        }

        // ensure that the user can then create a secure case note
        if (!securityUserContext.isOverrideRole("ADD_SENSITIVE_CASE_NOTES")) {
            throw new AccessDeniedException("User not allowed to create sensitive case notes");
        }

        final var parentType = parentNoteTypeOptional.get();
        final var type = caseNoteTypeRepository.findCaseNoteTypeByParentTypeAndType(parentType, newCaseNote.getSubType());

        if (type == null) {
            throw EntityNotFoundException.withId(newCaseNote.getSubType());
        }

        final var currentUsername = securityUserContext.getCurrentUsername();
        final var staffName = externalApiService.getUserFullName(currentUsername);

        final var locationId = newCaseNote.getLocationId() == null ? externalApiService.getOffenderLocation(offenderIdentifier) : newCaseNote.getLocationId();

        final var caseNote = OffenderCaseNote.builder()
                .noteText(newCaseNote.getText())
                .staffUsername(currentUsername)
                .staffName(staffName)
                .occurrenceDateTime(newCaseNote.getOccurrenceDateTime() == null ? LocalDateTime.now() : newCaseNote.getOccurrenceDateTime())
                .sensitiveCaseNoteType(type)
                .offenderIdentifier(offenderIdentifier)
                .locationId(locationId)
                .build();

        return mapper(repository.save(caseNote));
    }

    @Transactional
    public CaseNote amendCaseNote(@NotNull final String offenderIdentifier, @NotNull final String caseNoteIdentifier, @NotNull final String amendCaseNote) {
        if (isNotSensitiveCaseNote(caseNoteIdentifier)) {
            return mapper(externalApiService.amendOffenderCaseNote(offenderIdentifier, NumberUtils.toLong(caseNoteIdentifier), amendCaseNote), offenderIdentifier);
        }
        if (!securityUserContext.isOverrideRole("ADD_SENSITIVE_CASE_NOTES")) {
            throw new AccessDeniedException("User not allowed to view sensitive case notes");
        }

        final var offenderCaseNote = repository.findById(UUID.fromString(caseNoteIdentifier)).orElseThrow(() -> EntityNotFoundException.withId(caseNoteIdentifier));
        if (!offenderIdentifier.equals(offenderCaseNote.getOffenderIdentifier())) {
            throw EntityNotFoundException.withId(offenderIdentifier);
        }

        offenderCaseNote.addAmendment(amendCaseNote, securityUserContext.getCurrentUsername(), externalApiService.getUserFullName(securityUserContext.getCurrentUsername()));
        repository.save(offenderCaseNote);
        return mapper(offenderCaseNote);
    }

    public List<CaseNoteType> getCaseNoteTypes() {
        final var caseNoteTypes = externalApiService.getCaseNoteTypes();

        if (securityUserContext.isOverrideRole("VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")) {
            caseNoteTypes.addAll(getSensitiveCaseNotes());
        }

        return caseNoteTypes;
    }

    public List<CaseNoteType> getUserCaseNoteTypes() {
        final var userCaseNoteTypes = externalApiService.getUserCaseNoteTypes();
        if (securityUserContext.isOverrideRole("ADD_SENSITIVE_CASE_NOTES")) {
            userCaseNoteTypes.addAll(getSensitiveCaseNotes());
        }
        return userCaseNoteTypes;
    }

    private List<CaseNoteType> getSensitiveCaseNotes() {
        return caseNoteTypeRepository.findAll().stream()
                .collect(Collectors.groupingBy(SensitiveCaseNoteType::getParentType))
                .entrySet()
                .stream()
                .map(entry -> CaseNoteType.builder()
                        .code(entry.getKey().getType())
                        .description(entry.getKey().getDescription())
                        .activeFlag(entry.getKey().isActive() ? "Y" : "N")
                        .subCodes(entry.getValue().stream()
                                .map(st -> CaseNoteType.builder()
                                        .code(st.getType())
                                        .description(st.getDescription())
                                        .activeFlag(st.isActive() ? "Y" : "N")
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    public CaseNote getCaseNote(final String offenderIdentifier, final String caseNoteIdentifier) {
        if (isNotSensitiveCaseNote(caseNoteIdentifier)) {
            return mapper(externalApiService.getOffenderCaseNote(offenderIdentifier, NumberUtils.toLong(caseNoteIdentifier)), offenderIdentifier);
        }
        if (!securityUserContext.isOverrideRole("VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")) {
            throw new AccessDeniedException("User not allowed to view sensitive case notes");
        }
        return mapper(repository.findById(UUID.fromString(caseNoteIdentifier)).orElseThrow(() -> EntityNotFoundException.withId(caseNoteIdentifier)));
    }

    private boolean isNotSensitiveCaseNote(final String caseNoteIdentifier) {
        return NumberUtils.isDigits(caseNoteIdentifier);
    }
}
