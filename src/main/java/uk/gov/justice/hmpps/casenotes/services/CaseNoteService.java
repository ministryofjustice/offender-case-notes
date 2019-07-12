package uk.gov.justice.hmpps.casenotes.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;
import uk.gov.justice.hmpps.casenotes.dto.CaseNote;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote;
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Validated
public class CaseNoteService {

    private final OffenderCaseNoteRepository repository;
    private final SecurityUserContext securityUserContext;

    public CaseNoteService(OffenderCaseNoteRepository repository, SecurityUserContext securityUserContext) {
        this.repository = repository;
        this.securityUserContext = securityUserContext;
    }

    public Page<CaseNote> getCaseNotesByOffenderIdentifier(@NotNull final String offenderIdentifier, Pageable pageable) {
        final var filter = OffenderCaseNoteFilter.builder()
                .offenderIdentifier(offenderIdentifier)
                .build();

        return getCaseNotes(pageable, filter);
    }

    public Page<CaseNote> getCaseNotes(CaseNoteFilter caseNoteFilter, Pageable pageable) {
        final var filter = OffenderCaseNoteFilter.builder()
                .type(caseNoteFilter.getType())
                .subType(caseNoteFilter.getSubType())
                .locationId(caseNoteFilter.getLocationId())
                .staffUsername(caseNoteFilter.getStaffUsername())
                .startDate(caseNoteFilter.getStartDate())
                .endDate(caseNoteFilter.getEndDate())
                .build();

        return getCaseNotes(pageable, filter);
    }

    private Page<CaseNote> getCaseNotes(Pageable pageable, OffenderCaseNoteFilter filter) {
        final var pagedResults = repository.findAll(filter, pageable);

        final var caseNotes = pagedResults.getContent()
                .stream()
                .map(this::mapper)
                .collect(Collectors.toList());

        return new PageImpl<>(caseNotes, pageable, pagedResults.getTotalElements());
    }

    private CaseNote mapper(OffenderCaseNote cn) {
        return CaseNote.builder()
                .caseNoteId(cn.getId())
                .offenderIdentifier(cn.getOffenderIdentifier())
                .occurrenceDateTime(cn.getOccurrenceDateTime())
                .staffUsername(cn.getStaffUsername())
                .type(cn.getType())
                .subType(cn.getSubType())
                .text(cn.getNoteText())
                .creationDateTime(cn.getCreateDateTime())
                .amendments(cn.getAmendments().stream().map(
                        a -> CaseNoteAmendment.builder()
                                .authorUserName(a.getStaffUsername())
                                .additionalNoteText(a.getNoteText())
                                .caseNoteAmendmentId(a.getId())
                                .sequence(a.getAmendSequence())
                                .creationDateTime(a.getCreateDateTime())
                                .build()
                ).collect(Collectors.toList()))
                .locationId(cn.getLocationId())
                .build();
    }

    @Transactional
    public CaseNote createCaseNote(@NotNull final String offenderIdentifier, @NotNull @Valid final NewCaseNote newCaseNote) {
        final var caseNote = OffenderCaseNote.builder()
                .noteText(newCaseNote.getText())
                .staffUsername(securityUserContext.getCurrentUsername())
                .occurrenceDateTime(newCaseNote.getOccurrenceDateTime() == null ? LocalDateTime.now() : newCaseNote.getOccurrenceDateTime())
                .type(newCaseNote.getType())
                .subType(newCaseNote.getSubType())
                .offenderIdentifier(offenderIdentifier)
                .locationId(newCaseNote.getLocationId())
                .build();

        return mapper(repository.save(caseNote));
    }

    @Transactional
    public CaseNote amendCaseNote(@NotNull final String offenderIdentifier, @NotNull final Long caseNoteId, @NotNull final String amendCaseNote) {
        final var offenderCaseNote = repository.findById(caseNoteId).orElseThrow(EntityNotFoundException::new);

        if (!offenderIdentifier.equals(offenderCaseNote.getOffenderIdentifier())) {
            throw new EntityNotFoundException("Case Note not found for ID "+ caseNoteId);
        }

        offenderCaseNote.addAmendment(amendCaseNote, securityUserContext.getCurrentUsername());
        repository.save(offenderCaseNote);
        return mapper(offenderCaseNote);
    }
}
