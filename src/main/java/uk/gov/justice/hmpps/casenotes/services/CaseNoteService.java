package uk.gov.justice.hmpps.casenotes.services;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;
import uk.gov.justice.hmpps.casenotes.dto.CaseNote;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteType;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNoteType;
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNoteType;
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType;
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType;
import uk.gov.justice.hmpps.casenotes.repository.CaseNoteTypeRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteAmendmentRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;
import uk.gov.justice.hmpps.casenotes.repository.ParentCaseNoteTypeRepository;

import javax.persistence.EntityExistsException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.springframework.data.domain.Sort.Direction.ASC;

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
    private final ParentCaseNoteTypeRepository parentCaseNoteTypeRepository;
    private final SecurityUserContext securityUserContext;
    private final ExternalApiService externalApiService;
    private final CaseNoteTypeMerger caseNoteTypeMerger;
    private final TelemetryClient telemetryClient;

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
            final var pagedNotes = externalApiService.getOffenderCaseNotes(offenderIdentifier, caseNoteFilter, 10000, 0, "caseNoteId", ASC);

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
            .eventId(cn.getEventId())
            .offenderIdentifier(cn.getOffenderIdentifier())
            .occurrenceDateTime(cn.getOccurrenceDateTime())
            .authorUserId(cn.getAuthorUserId())
            .authorName(cn.getAuthorName())
            .type(parentType.getType())
            .typeDescription(parentType.getDescription())
            .subType(cn.getSensitiveCaseNoteType().getType())
            .subTypeDescription(cn.getSensitiveCaseNoteType().getDescription())
            .source(SERVICE_NAME) // Indicates its a Offender Case Note Service Type
            .text(cn.getNoteText())
            .creationDateTime(cn.getCreateDateTime())
            .amendments(cn.getAmendments().stream().map(
                a -> CaseNoteAmendment.builder()
                    .authorUserName(a.getAuthorUsername())
                    .authorUserId(a.getAuthorUserId())
                    .authorName(a.getAuthorName())
                    .additionalNoteText(a.getNoteText())
                    .caseNoteAmendmentId(a.getId())
                    .creationDateTime(a.getCreateDateTime())
                    .build()
            ).collect(Collectors.toList()))
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
            .text(cn.getOriginalNoteText())
            .creationDateTime(cn.getCreationDateTime())
            .amendments(cn.getAmendments().stream().map(
                a -> CaseNoteAmendment.builder()
                    .authorName(a.getAuthorName())
                    .authorUserId(a.getAuthorUserId())
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

        final var author = securityUserContext.getCurrentUser();
        final var staffName = externalApiService.getUserFullName(author.getUsername());

        final var locationId = newCaseNote.getLocationId() == null ? externalApiService.getOffenderLocation(offenderIdentifier) : newCaseNote.getLocationId();

        final var caseNote = OffenderCaseNote.builder()
            .noteText(newCaseNote.getText())
            .authorUsername(author.getUsername())
            .authorUserId(author.getUserId())
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

        final var author = securityUserContext.getCurrentUser();
        final var authorFullName = externalApiService.getUserFullName(author.getUsername());

        offenderCaseNote.addAmendment(amendCaseNote.getText(), author.getUsername(), authorFullName, author.getUserId());
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
            .source(SERVICE_NAME)
            .subCodes(parentNoteType.getSubTypes().stream()
                .filter(t -> allTypes || t.isActive())
                .map(st -> CaseNoteType.builder()
                    .code(st.getType())
                    .description(st.getDescription())
                    .source(SERVICE_NAME)
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

    @Transactional
    public int deleteCaseNotesForOffender(final String offenderIdentifier) {
        repository.deleteOffenderCaseNoteAmendmentsByOffenderIdentifier(offenderIdentifier);
        final var deletedCaseNotesCount = repository.deleteOffenderCaseNoteByOffenderIdentifier(offenderIdentifier);
        log.info("Deleted {} case notes for offender identifier {}", deletedCaseNotesCount, offenderIdentifier);
        telemetryClient.trackEvent("OffenderDelete", Map.of("offenderNo", offenderIdentifier, "count", valueOf(deletedCaseNotesCount)), null);
        return deletedCaseNotesCount;
    }

    @Transactional
    @PreAuthorize("hasRole('DELETE_SENSITIVE_CASE_NOTES')")
    public void softDeleteCaseNote(final String offenderIdentifier, final String caseNoteId) {
        if (isNotSensitiveCaseNote(caseNoteId)) {
            throw new ValidationException("Case note id not a sensitive case note, please delete through NOMIS");
        }
        final var caseNote = repository.findById(UUID.fromString(caseNoteId)).orElseThrow(() -> new EntityNotFoundException("Case note not found"));
        if (!caseNote.getOffenderIdentifier().equalsIgnoreCase(offenderIdentifier)) {
            throw new ValidationException("case note id not connected with offenderIdentifier");
        }
        repository.deleteById(UUID.fromString(caseNoteId));
        telemetryClient.trackEvent("SecureCaseNoteSoftDelete",
            Map.of("userName", securityUserContext.getCurrentUser().getUsername(),
                "offenderId", offenderIdentifier,
                "case note id", valueOf(caseNoteId)),
            null);
    }

    @Transactional
    @PreAuthorize("hasRole('DELETE_SENSITIVE_CASE_NOTES')")
    public void softDeleteCaseNoteAmendment(final String offenderIdentifier, final Long caseNoteAmendmentId) {
        final var caseNoteAmendment = amendmentRepository.findById(caseNoteAmendmentId).orElseThrow(() -> new EntityNotFoundException("Case note amendment not found"));

        if (!caseNoteAmendment.getCaseNote().getOffenderIdentifier().equalsIgnoreCase(offenderIdentifier)) {
            throw new ValidationException("case note amendment id not connected with offenderIdentifier");
        }
        amendmentRepository.deleteById(caseNoteAmendmentId);

        telemetryClient.trackEvent("SecureCaseNoteAmendmentSoftDelete",
            Map.of("userName", securityUserContext.getCurrentUser().getUsername(),
                "offenderId", offenderIdentifier,
                "case note amendment id", valueOf(caseNoteAmendmentId)),
            null);
    }
}
