package uk.gov.justice.hmpps.casenotes.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.stream.Collectors;

import static org.springframework.data.domain.Sort.Direction.ASC;

@Service
@Transactional(readOnly = true)
@Validated
public class CaseNoteService {

    private final OffenderCaseNoteRepository repository;
    private final CaseNoteTypeRepository caseNoteTypeRepository;
    private final ParentCaseNoteTypeRepository parentCaseNoteTypeRepository;
    private final SecurityUserContext securityUserContext;
    private final ExternalApiService externalApiService;

    public CaseNoteService(OffenderCaseNoteRepository repository, CaseNoteTypeRepository caseNoteTypeRepository, ParentCaseNoteTypeRepository parentCaseNoteTypeRepository, SecurityUserContext securityUserContext, ExternalApiService externalApiService) {
        this.repository = repository;
        this.caseNoteTypeRepository = caseNoteTypeRepository;
        this.parentCaseNoteTypeRepository = parentCaseNoteTypeRepository;
        this.securityUserContext = securityUserContext;
        this.externalApiService = externalApiService;
    }

    public Page<CaseNote> getCaseNotes(final String offenderIdentifier, final CaseNoteFilter caseNoteFilter, final Pageable pageable) {

        final var sensitiveCaseNotes = new ArrayList<CaseNote>();

        if (SecurityUserContext.hasRoles("VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")) {

            final var filter = OffenderCaseNoteFilter.builder()
                    .offenderIdentifier(offenderIdentifier)
                    .type(caseNoteFilter.getType())
                    .subType(caseNoteFilter.getSubType())
                    .locationId(caseNoteFilter.getLocationId())
                    .staffUsername(caseNoteFilter.getStaffUsername())
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

        Page<CaseNote> caseNotes;
        if (sensitiveCaseNotes.isEmpty()) {
            // Just degate to elite2 for data
            final var pagedNotes = externalApiService.getOffenderCaseNotes(offenderIdentifier, caseNoteFilter, pageable.getPageSize(), pageable.getPageNumber(), sortField, direction);

            final var dtoNotes = translateToDto(pagedNotes, offenderIdentifier);
            caseNotes = new PageImpl<>(dtoNotes, pageable, pagedNotes.getTotalElements());

        } else {
            // There are both case note sources.  Combine
            final var pagedNotes = externalApiService.getOffenderCaseNotes(offenderIdentifier, caseNoteFilter, 1000, 0, "caseNoteId", ASC);

            final var dtoNotes = translateToDto(pagedNotes, offenderIdentifier);

            dtoNotes.addAll(sensitiveCaseNotes);

            final var sortedList = sortByFieldName(dtoNotes, sortField, direction);

            int toIndex = (int) (pageable.getOffset() + pageable.getPageSize());
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
    private static List<CaseNote> sortByFieldName(List<CaseNote> list, String fieldName, Sort.Direction direction)  {
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
        } catch (NoSuchFieldException e) {
            return list;
        }
    }

    private CaseNote mapper(final OffenderCaseNote cn) {
        final var parentType = cn.getSensitiveCaseNoteType().getParentType();
        return CaseNote.builder()
                .caseNoteId(cn.getId())
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
                .caseNoteId(cn.getCaseNoteId())
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
        final var parentType = parentCaseNoteTypeRepository.findById(newCaseNote.getType()).orElseThrow(() -> EntityNotFoundException.withId(newCaseNote.getType()));
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
    public CaseNote amendCaseNote(@NotNull final String offenderIdentifier, @NotNull final Long caseNoteId, @NotNull final String amendCaseNote) {
        final var offenderCaseNote = repository.findById(caseNoteId).orElseThrow(() -> EntityNotFoundException.withId(caseNoteId));

        if (!offenderIdentifier.equals(offenderCaseNote.getOffenderIdentifier())) {
            throw EntityNotFoundException.withId(offenderIdentifier);
        }

        offenderCaseNote.addAmendment(amendCaseNote, securityUserContext.getCurrentUsername(), externalApiService.getUserFullName(securityUserContext.getCurrentUsername()));
        repository.save(offenderCaseNote);
        return mapper(offenderCaseNote);
    }

    public List<CaseNoteType> getCaseNoteTypes() {
        final var caseNoteTypes = externalApiService.getCaseNoteTypes();

        if (SecurityUserContext.hasRoles("VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")) {
            caseNoteTypes.addAll(getSensitiveCaseNotes());
        }

        return caseNoteTypes;
    }

    public List<CaseNoteType> getUserCaseNoteTypes() {
        final var userCaseNoteTypes = externalApiService.getUserCaseNoteTypes();
        if (SecurityUserContext.hasRoles("ADD_SENSITIVE_CASE_NOTES")) {
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
}
