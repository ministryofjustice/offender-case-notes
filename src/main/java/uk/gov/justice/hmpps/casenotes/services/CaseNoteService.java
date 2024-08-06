package uk.gov.justice.hmpps.casenotes.services;

import com.microsoft.applicationinsights.TelemetryClient;
import jakarta.persistence.EntityExistsException;
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
import uk.gov.justice.hmpps.casenotes.dto.CaseNote;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteTypeDto;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNoteType;
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNoteType;
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.model.CaseNoteType;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType;
import uk.gov.justice.hmpps.casenotes.repository.CaseNoteTypeRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteAmendmentRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;
import uk.gov.justice.hmpps.casenotes.repository.ParentCaseNoteTypeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Optional.ofNullable;
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

    @SuppressWarnings("unchecked")
    private static List<CaseNote> sortByFieldName(
        final List<CaseNote> list,
        final String fieldName,
        final Sort.Direction direction
    ) {
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
            .amendments(cn.getAmendments().stream().map(
                a -> CaseNoteAmendment.builder()
                    .authorUserName(a.getAuthorUsername())
                    .authorUserId(a.getAuthorUserId())
                    .authorName(a.getAuthorName())
                    .additionalNoteText(a.getNoteText())
                    .caseNoteAmendmentId(a.getId())
                    .creationDateTime(a.getCreateDateTime())
                    .build()
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
            .amendments(cn.getAmendments().stream().map(
                a -> CaseNoteAmendment.builder()
                    .authorName(a.getAuthorName())
                    .authorUserId(a.getAuthorUserId())
                    .authorUserName(a.getAuthorUsername())
                    .additionalNoteText(a.getAdditionalNoteText())
                    .creationDateTime(a.getCreationDateTime())
                    .build()
            ).toList())
            .locationId(cn.getAgencyId())
            .build();
    }

    @Transactional
    public CaseNote createCaseNote(
        @NotNull final String offenderIdentifier,
        @NotNull @Valid final NewCaseNote newCaseNote
    ) {
        final var type = caseNoteTypeRepository.findCaseNoteTypeByParentTypeTypeAndType(
            newCaseNote.getType(),
            newCaseNote.getSubType()
        );

        // If we don't have the type locally then won't be secure, so delegate to prison-api
        if (type == null) {
            return mapper(externalApiService.createCaseNote(offenderIdentifier, newCaseNote), offenderIdentifier);
        }

        // ensure that the user can then create the case note
        if (type.isRestrictedUse() && !isAllowedToCreateRestrictedCaseNote()) {
            throw new AccessDeniedException("User not allowed to create this case note type [" + type + "]");
        }

        // ensure that the case note type is active
        if (!type.getParentType().isActive() || !type.isActive()) {
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
            .occurrenceDateTime(newCaseNote.getOccurrenceDateTime() == null ? LocalDateTime.now() : newCaseNote.getOccurrenceDateTime())
            .caseNoteType(type)
            .offenderIdentifier(offenderIdentifier)
            .locationId(locationId)
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
        @NotNull @Valid final UpdateCaseNote amendCaseNote
    ) {
        if (isNotSensitiveCaseNote(caseNoteIdentifier)) {
            return mapper(externalApiService.amendOffenderCaseNote(
                offenderIdentifier,
                NumberUtils.toLong(caseNoteIdentifier),
                amendCaseNote
            ), offenderIdentifier);
        }

        final var offenderCaseNote = repository.findById(UUID.fromString(caseNoteIdentifier))
            .orElseThrow(() -> EntityNotFoundException.withId(caseNoteIdentifier));
        if (offenderCaseNote.getCaseNoteType().isRestrictedUse() && !isAllowedToCreateRestrictedCaseNote()) {
            throw new AccessDeniedException("User not allowed to amend this case note type [" + offenderCaseNote.getCaseNoteType() + "]");
        }

        if (!offenderIdentifier.equals(offenderCaseNote.getOffenderIdentifier())) {
            throw EntityNotFoundException.withId(offenderIdentifier);
        }

        final var username = securityUserContext.getCurrentUser().getUsername();
        final var userDetails = externalApiService.getUserDetails(username);
        final var authorName = userDetails
            .map(user -> isNullOrEmpty(user.getName()) ? username : user.getName())
            .orElse(username);
        final var authorUserId = userDetails
            .map(user -> isNullOrEmpty(user.getUserId()) ? username : user.getUserId())
            .orElse(username);

        offenderCaseNote.addAmendment(amendCaseNote.getText(), username, authorName, authorUserId);
        repository.save(offenderCaseNote);
        return mapper(offenderCaseNote);
    }

    public List<CaseNoteTypeDto> getCaseNoteTypes() {
        final var caseNoteTypes = externalApiService.getCaseNoteTypes();
        return caseNoteTypeMerger.mergeAndSortList(
            caseNoteTypes,
            getCaseNoteTypes(true, isAllowedToViewOrCreateSensitiveCaseNote(), null)
        );
    }

    public List<CaseNoteTypeDto> getUserCaseNoteTypes() {
        final var userCaseNoteTypes = externalApiService.getUserCaseNoteTypes();
        return caseNoteTypeMerger.mergeAndSortList(
            userCaseNoteTypes,
            getCaseNoteTypes(false, null, isAllowedToCreateRestrictedCaseNote())
        );
    }


    private List<CaseNoteTypeDto> getCaseNoteTypes(
        final boolean allTypes,
        final Boolean sensitiveAllowed,
        final Boolean restrictedAllowed
    ) {
        return parentCaseNoteTypeRepository.findAll().stream()
            .filter(t -> allTypes || t.isActive())
            .map(st -> transform(st, allTypes, sensitiveAllowed, restrictedAllowed))
            .filter(pt -> !pt.getSubCodes().isEmpty())
            .collect(Collectors.toList());
    }

    private CaseNoteTypeDto transform(
        final ParentNoteType parentNoteType,
        final boolean allTypes,
        final Boolean sensitiveAllowed,
        final Boolean restrictedAllowed
    ) {
        return CaseNoteTypeDto.builder()
            .code(parentNoteType.getType())
            .description(parentNoteType.getDescription())
            .activeFlag(parentNoteType.isActive() ? "Y" : "N")
            .source(SERVICE_NAME)
            .subCodes(parentNoteType.getSubTypes().stream()
                .filter(t -> allTypes || t.isActive())
                .filter(t -> restrictedAllowed == null || (restrictedAllowed || !t.isRestrictedUse()))
                .filter(t -> sensitiveAllowed == null || (sensitiveAllowed || !t.isSensitive()))
                .map(st -> CaseNoteTypeDto.builder()
                    .code(st.getType())
                    .description(st.getDescription())
                    .source(SERVICE_NAME)
                    .activeFlag(st.isActive() ? "Y" : "N")
                    .sensitive(st.isSensitive())
                    .restrictedUse(st.isRestrictedUse())
                    .build())
                .toList())
            .build();
    }

    public CaseNote getCaseNote(final String offenderIdentifier, final String caseNoteIdentifier) {
        if (isNotSensitiveCaseNote(caseNoteIdentifier)) {
            return mapper(externalApiService.getOffenderCaseNote(
                offenderIdentifier,
                NumberUtils.toLong(caseNoteIdentifier)
            ), offenderIdentifier);
        }

        final var caseNote = repository.findById(UUID.fromString(caseNoteIdentifier))
            .orElseThrow(() -> EntityNotFoundException.withId(caseNoteIdentifier));

        if (caseNote.getCaseNoteType().isSensitive() && !isAllowedToViewOrCreateSensitiveCaseNote()) {
            throw new AccessDeniedException("User not allowed to view sensitive case notes");
        }
        return mapper(caseNote);
    }

    private boolean isNotSensitiveCaseNote(final String caseNoteIdentifier) {
        return NumberUtils.isDigits(caseNoteIdentifier);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MAINTAIN_REF_DATA', 'SYSTEM_USER')")
    public CaseNoteTypeDto createCaseNoteType(@NotNull @Valid final NewCaseNoteType newCaseNoteType) {

        final var parentNoteTypeOptional = parentCaseNoteTypeRepository.findById(newCaseNoteType.getType());

        if (parentNoteTypeOptional.isPresent()) {
            throw new EntityExistsException(newCaseNoteType.getType());
        }

        final var parentNoteType = parentCaseNoteTypeRepository.save(ParentNoteType.builder()
            .type(newCaseNoteType.getType())
            .description(newCaseNoteType.getDescription())
            .active(newCaseNoteType.isActive())
            .build());

        return transform(parentNoteType, true, true, true);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MAINTAIN_REF_DATA', 'SYSTEM_USER')")
    public CaseNoteTypeDto createCaseNoteSubType(
        final String parentType,
        @NotNull @Valid final NewCaseNoteType newCaseNoteType
    ) {
        final var parentNoteType =
            parentCaseNoteTypeRepository.findById(parentType).orElseThrow(EntityNotFoundException.withId(parentType));

        if (parentNoteType.getSubType(newCaseNoteType.getType()).isPresent()) {
            throw new EntityExistsException(newCaseNoteType.getType());
        }
        parentNoteType.getSubTypes().add(
            CaseNoteType.builder()
                .type(newCaseNoteType.getType())
                .description(newCaseNoteType.getDescription())
                .active(newCaseNoteType.isActive())
                .parentType(parentNoteType)
                .sensitive(newCaseNoteType.isSensitive())
                .restrictedUse(newCaseNoteType.isRestrictedUse())
                .build()
        );

        return transform(parentNoteType, true, true, true);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MAINTAIN_REF_DATA', 'SYSTEM_USER')")
    public CaseNoteTypeDto updateCaseNoteType(final String parentType, @NotNull @Valid final UpdateCaseNoteType body) {
        final var parentNoteType =
            parentCaseNoteTypeRepository.findById(parentType).orElseThrow(EntityNotFoundException.withId(parentType));
        parentNoteType.update(body.getDescription(), body.isActive());
        return transform(parentNoteType, true, true, true);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MAINTAIN_REF_DATA', 'SYSTEM_USER')")
    public CaseNoteTypeDto updateCaseNoteSubType(
        final String parentType,
        final String subType,
        @NotNull @Valid final UpdateCaseNoteType body
    ) {

        final var parentNoteType =
            parentCaseNoteTypeRepository.findById(parentType).orElseThrow(EntityNotFoundException.withId(parentType));
        final var existingSubType =
            parentNoteType.getSubType(subType).orElseThrow(EntityNotFoundException.withId(parentType + " " + subType));
        existingSubType.update(body.getDescription(), body.isActive(), body.isSensitive(), body.isRestrictedUse());
        return transform(parentNoteType, true, true, true);
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
        if (isNotSensitiveCaseNote(caseNoteId)) {
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

    @Transactional
    @PreAuthorize("hasRole('DELETE_SENSITIVE_CASE_NOTES')")
    public void softDeleteCaseNoteAmendment(final String offenderIdentifier, final Long caseNoteAmendmentId) {
        final var caseNoteAmendment = amendmentRepository.findById(caseNoteAmendmentId)
            .orElseThrow(() -> new EntityNotFoundException("Case note amendment not found"));

        if (!caseNoteAmendment.getCaseNote().getOffenderIdentifier().equalsIgnoreCase(offenderIdentifier)) {
            throw new ValidationException("case note amendment id not connected with offenderIdentifier");
        }
        amendmentRepository.deleteById(caseNoteAmendmentId);

        telemetryClient.trackEvent(
            "SecureCaseNoteAmendmentSoftDelete",
            Map.of("userName", securityUserContext.getCurrentUser().getUsername(),
                "offenderId", offenderIdentifier,
                "case note amendment id", valueOf(caseNoteAmendmentId)
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
