package uk.gov.justice.hmpps.casenotes.legacy.service;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.casenotes.legacy.dto.SARCaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.legacy.dto.SubjectAccessRequestData;
import uk.gov.justice.hmpps.casenotes.legacy.filters.SAROffenderCaseNoteAmendmentFilter;
import uk.gov.justice.hmpps.casenotes.legacy.filters.SAROffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.legacy.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.legacy.model.OffenderCaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.legacy.repository.OffenderCaseNoteAmendmentRepository;
import uk.gov.justice.hmpps.casenotes.legacy.repository.OffenderCaseNoteRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
@Slf4j
public class SubjectAccessRequestService {
    private final OffenderCaseNoteRepository repository;

    private final OffenderCaseNoteAmendmentRepository amendmentRepository;

    public List< SubjectAccessRequestData> getCaseNotes(final String offenderIdentifier, final LocalDate fromDate, final LocalDate toDate) {

        final var sarOffenderCaseNoteFilter = new SAROffenderCaseNoteFilter(offenderIdentifier, fromDate, toDate);

        // Case 1: Where a case note has an amendment and the parent case note is created within the date range specified
        var offenderCaseNoteList = repository.findAll(sarOffenderCaseNoteFilter);
        // Case 2: Where an amendment to a case note is created within the date range specified
        final var offenderCaseNoteAmendmentFilter = new SAROffenderCaseNoteAmendmentFilter(offenderIdentifier, fromDate, toDate);
        var offenderCaseNoteAmendmentList = amendmentRepository.findAll(offenderCaseNoteAmendmentFilter);
        // Pull all CaseNotes related to Amendments created with in the date range specified
        var offenderNotesFromAmendment = offenderCaseNoteAmendmentList.stream()
                .map(OffenderCaseNoteAmendment::getCaseNote).collect(Collectors.toSet());
        //Add Amendments from Case1 and Case2
        offenderCaseNoteList.addAll(offenderNotesFromAmendment);
        //remove duplicate case notes
        offenderCaseNoteList = offenderCaseNoteList.stream().distinct().toList();

        return offenderCaseNoteList.stream()
                .map(this::toSubjectAccessRequestContent)
                .sorted(comparing(SubjectAccessRequestData::getCreationDateTime).reversed())
                .toList();
    }

    private SubjectAccessRequestData toSubjectAccessRequestContent(final OffenderCaseNote cn) {

        final var parentType = cn.getSubType().getType();

        return   SubjectAccessRequestData.builder()
                .authorName(cn.getAuthorName())
                .type(parentType.getCode())
                .subType(cn.getSubType().getCode())
                .text(cn.getText())
                .creationDateTime(cn.getCreatedAt())
                .amendments(cn.getAmendments().stream().map(
                        a -> SARCaseNoteAmendment.builder()
                                .authorName(a.getAuthorName())
                                .additionalNoteText(a.getText())
                                .creationDateTime(a.getCreatedAt())
                                .build()
                ).sorted(comparing(SARCaseNoteAmendment::getCreationDateTime).reversed()).toList()).build();
    }
}
