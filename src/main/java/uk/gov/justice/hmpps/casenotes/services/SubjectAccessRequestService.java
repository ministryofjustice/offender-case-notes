package uk.gov.justice.hmpps.casenotes.services;


import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.dto.SubjectAccessRequestContent;
import uk.gov.justice.hmpps.casenotes.filters.SAROffenderCaseNoteAmendmentFilter;
import uk.gov.justice.hmpps.casenotes.filters.SAROffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteAmendmentRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
@Slf4j
public class SubjectAccessRequestService {
    private final OffenderCaseNoteRepository repository;

    private final OffenderCaseNoteAmendmentRepository amendmentRepository;

    private final TelemetryClient telemetryClient;

    public List<SubjectAccessRequestContent> getCaseNotes(final String offenderIdentifier, final LocalDate fromDate, final LocalDate toDate) {

        final List<SubjectAccessRequestContent> sensitiveCaseNotes;

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

        sensitiveCaseNotes = offenderCaseNoteList.stream()
                .map(this::toSubjectAccessRequestContent)
                .toList();
        log.debug("{} Case notes for Subject access request fetched for offender identifier {}", sensitiveCaseNotes.size(), offenderIdentifier);
        telemetryClient.trackEvent("SAROffenderCaseNotes", Map.of("offenderNo", offenderIdentifier, "fromDate", valueOf(fromDate),"toDate", valueOf(toDate),"count", valueOf(sensitiveCaseNotes.size())), null);
        return sensitiveCaseNotes;
    }

    private SubjectAccessRequestContent toSubjectAccessRequestContent(final OffenderCaseNote cn) {

        final var parentType = cn.getCaseNoteType().getParentType();

        return  SubjectAccessRequestContent.builder()
                .authorName(cn.getAuthorName())
                .type(parentType.getType())
                .subType(cn.getCaseNoteType().getType())
                .text(cn.getNoteText())
                .creationDateTime(cn.getCreateDateTime())
                .amendments(cn.getAmendments().stream().map(
                        a -> CaseNoteAmendment.builder()
                                .authorName(a.getAuthorName())
                                .additionalNoteText(a.getNoteText())
                                .creationDateTime(a.getCreateDateTime())
                                .build()
                ).collect(Collectors.toList())).build();
    }
}
