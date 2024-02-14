package uk.gov.justice.hmpps.casenotes.services;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.UserIdUser;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNote;
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.model.CaseNoteType;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType;
import uk.gov.justice.hmpps.casenotes.repository.CaseNoteTypeRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteAmendmentRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;
import uk.gov.justice.hmpps.casenotes.repository.ParentCaseNoteTypeRepository;

import jakarta.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.*;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CaseNoteServiceTest {
    @Mock
    private OffenderCaseNoteRepository repository;
    @Mock
    private OffenderCaseNoteAmendmentRepository amendmentRepository;
    @Mock
    private CaseNoteTypeRepository caseNoteTypeRepository;
    @Mock
    private ParentCaseNoteTypeRepository parentCaseNoteTypeRepository;
    @Mock
    private SecurityUserContext securityUserContext;
    @Mock
    private ExternalApiService externalApiService;
    @Mock
    private CaseNoteTypeMerger caseNoteTypeMerger;
    @Mock
    private TelemetryClient telemetryClient;

    private CaseNoteService caseNoteService;

    @BeforeEach
    public void setUp() {
        caseNoteService = new CaseNoteService(repository, amendmentRepository, caseNoteTypeRepository, parentCaseNoteTypeRepository, securityUserContext, externalApiService, caseNoteTypeMerger, telemetryClient);
    }

    @Test
    public void createCaseNote_callElite2() {
        when(caseNoteTypeRepository.findCaseNoteTypeByParentType_TypeAndType(anyString(), anyString())).thenReturn(null);

        final var nomisCaseNote = createNomisCaseNote();
        when(externalApiService.createCaseNote(anyString(), any())).thenReturn(nomisCaseNote);

        final var caseNote = caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("SUB").build());

        assertThat(caseNote).isEqualToIgnoringGivenFields(nomisCaseNote, "authorUsername", "locationId", "text", "caseNoteId", "authorUserId", "eventId", "sensitive");
        assertThat(caseNote.getText()).isEqualTo("original");
        assertThat(caseNote.getAuthorUserId()).isEqualTo("23456");
        assertThat(caseNote.getLocationId()).isEqualTo("agency");
        assertThat(caseNote.getCaseNoteId()).isEqualTo("12345");
        assertThat(caseNote.getEventId()).isEqualTo(12345);
        verify(caseNoteTypeRepository).findCaseNoteTypeByParentType_TypeAndType("type", "SUB");
    }

    @Test
    public void createCaseNote_noAddRole() {
        when(caseNoteTypeRepository.findCaseNoteTypeByParentType_TypeAndType(anyString(), anyString())).thenReturn(CaseNoteType.builder().build());
        when(securityUserContext.isOverrideRole(anyString(), anyString())).thenReturn(Boolean.FALSE);

        assertThatThrownBy(() -> caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("SUB").build())).isInstanceOf(AccessDeniedException.class);

        verify(securityUserContext).isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES");
    }

    @Test
    public void createCaseNote() {
        final var noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        when(caseNoteTypeRepository.findCaseNoteTypeByParentType_TypeAndType(anyString(), anyString())).thenReturn(noteType);
        when(securityUserContext.isOverrideRole(anyString(), anyString())).thenReturn(Boolean.TRUE);
        when(securityUserContext.getCurrentUser()).thenReturn(new UserIdUser("someuser", "userId"));
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        when(repository.save(any())).thenReturn(offenderCaseNote);

        final var createdNote = caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("sub").build());
        assertThat(createdNote).isEqualToIgnoringGivenFields(offenderCaseNote,
                "caseNoteId", "type", "typeDescription", "subType", "subTypeDescription", "source", "creationDateTime", "text", "amendments", "sensitive");
        assertThat(createdNote.getText()).isEqualTo("HELLO");
    }

    @Test
    public void getCaseNote_noAddRole() {
        final var noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        when(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote));

        assertThatThrownBy(() -> caseNoteService.getCaseNote("12345", UUID.randomUUID().toString())).isInstanceOf(AccessDeniedException.class);

        verify(securityUserContext).isOverrideRole("POM", "VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES");
    }

    @Test
    public void getCaseNote_notFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caseNoteService.getCaseNote("12345", UUID.randomUUID().toString())).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    public void getCaseNote() {
        final var noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        when(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote));
        when(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(Boolean.TRUE);

        final var caseNote = caseNoteService.getCaseNote("12345", UUID.randomUUID().toString());
        assertThat(caseNote).isEqualToIgnoringGivenFields(offenderCaseNote,
                "caseNoteId", "type", "typeDescription", "subType", "subTypeDescription", "source", "creationDateTime", "authorUsername", "authorName", "text", "amendments", "sensitive");
        assertThat(caseNote.getText()).isEqualTo("HELLO");
    }

    @Test
    public void getCaseNote_callElite2() {
        final var nomisCaseNote = createNomisCaseNote();
        when(externalApiService.getOffenderCaseNote(anyString(), anyLong())).thenReturn(nomisCaseNote);

        final var caseNote = caseNoteService.getCaseNote("12345", "21455");

        assertThat(caseNote).isEqualToIgnoringGivenFields(nomisCaseNote, "authorUsername", "locationId", "text", "caseNoteId", "authorUserId", "eventId", "sensitive");
        assertThat(caseNote.getText()).isEqualTo("original");
        assertThat(caseNote.getAuthorUserId()).isEqualTo("23456");
        assertThat(caseNote.getLocationId()).isEqualTo("agency");
        assertThat(caseNote.getCaseNoteId()).isEqualTo("12345");
        assertThat(caseNote.getEventId()).isEqualTo(12345);
    }

    @Test
    public void getCaseNotes_GetNotesFromRootTypeAndSubType_NoSensitiveCaseNotes_ReturnCaseNotesFromPrisonApi() {
        final var offenderCaseNote = createNomisCaseNote("someType", "someSubType");
        final var pageable = PageRequest.of(0,10, Direction.DESC,"occurrenceDateTime");
        final var list =Collections.singletonList(offenderCaseNote);
        when(externalApiService.getOffenderCaseNotes(anyString(),any(),any())).thenReturn(new PageImpl<>(list, pageable, list.size()));
        when(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(Boolean.TRUE);

        final var filter = new CaseNoteFilter("someType","someSUbType",LocalDateTime.now(),LocalDateTime.now().plusDays(1),"Location","user",Collections.emptyList());
        final var caseNotes = caseNoteService.getCaseNotes("12345", filter, pageable);
        final var caseNote = caseNotes.getContent().getFirst();
        assertThat(caseNotes.getContent().getFirst()).isEqualToIgnoringGivenFields(offenderCaseNote,
            "authorUsername", "locationId", "text", "caseNoteId", "authorUserId", "eventId", "sensitive");
        assertThat(caseNote.getType()).isEqualTo("someType");
        assertThat(caseNote.getSubType()).isEqualTo("someSubType");
    }

    @Test
    public void getCaseNotes_GetNotesFromRootTypeAndSubType_HasSensitiveCaseNotes_ReturnCombinedCaseNotes() {
        final var nomisCaseNote = createNomisCaseNote("someType", "someSubType");
        final var noteType = CaseNoteType.builder().type("someSubType").parentType(ParentNoteType.builder().type("someType").build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        final var pageable = PageRequest.of(0,10, Direction.DESC,"occurrenceDateTime");
        final var apiResponseList =Collections.singletonList(nomisCaseNote);
        final var repositoryResponseList =Collections.singletonList((offenderCaseNote));
        when(repository.findAll(ArgumentMatchers.<Specification<OffenderCaseNote>>any())).thenReturn(repositoryResponseList);
        when(externalApiService.getOffenderCaseNotes(anyString(),any(),any())).thenReturn(new PageImpl<>(apiResponseList, pageable, apiResponseList.size()));
        when(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(Boolean.TRUE);



        final var filter = new CaseNoteFilter("someType","someSubType",LocalDateTime.now(),LocalDateTime.now().plusDays(1),"Location","user",Collections.emptyList());
        final var caseNotes = caseNoteService.getCaseNotes("12345", filter, pageable).getContent();
        assertThat(caseNotes.size()).isEqualTo(2);
        assert(caseNotes.stream().allMatch(x->x.getType().equals("someType")&&x.getSubType().equals("someSubType")));
        // offenderCaseNote exist
        assert(caseNotes.stream().anyMatch(x->x.getText().equals("HELLO")));
        // nomisCaseNote exist
        assert(caseNotes.stream().anyMatch(x->x.getText().equals("original")));
    }

    @Test
    public void getCaseNotes_GetNotesForRootTypeAndAdditionalTypes_ReturnAllCaseNotes() {
        final var offenderIdentifier="12345";
        final var pageable = PageRequest.of(0,10, Direction.DESC,"occurrenceDateTime");
        when(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(Boolean.TRUE);

        final var rootNoteType = CaseNoteType.builder().type("someSubType").parentType(ParentNoteType.builder().type("someType").build()).build();
        final var rootTypeFilter = new CaseNoteFilter("someType","someSubType",LocalDateTime.now(),LocalDateTime.now().plusDays(1),"Location","user",Collections.singletonList("someAddType+someAddSubType"));
        final var rootTypeOffenderCaseNoteFilter = OffenderCaseNoteFilter.builder()
            .offenderIdentifier(offenderIdentifier)
            .type("someType")
            .subType("someSubType")
            .locationId(rootTypeFilter.getLocationId())
            .authorUsername(rootTypeFilter.getAuthorUsername())
            .startDate(rootTypeFilter.getStartDate())
            .endDate(rootTypeFilter.getEndDate())
            .excludeSensitive(false)
            .build();

        final var offenderCaseNote = createOffenderCaseNote(rootNoteType);
        final var rootTypeRepositoryResponseList =Collections.singletonList((offenderCaseNote));
        when(repository.findAll(rootTypeOffenderCaseNoteFilter)).thenReturn(rootTypeRepositoryResponseList);

        final var rootTypeNomisCaseNote = createNomisCaseNote("someType", "someSubType");
        final var rootTypeApiResponseList =Collections.singletonList(rootTypeNomisCaseNote);
        final var addtionalTypeNomisCaseNote = createNomisCaseNote("someAddType", "someAddSubType");
        final var addtionalTypeApiResponseList =Collections.singletonList(addtionalTypeNomisCaseNote);
        when(externalApiService.getOffenderCaseNotes(anyString(),any(),any())).thenAnswer(invocation ->{
            var filter =(CaseNoteFilter) invocation.getArguments()[1];
            if(Objects.requireNonNull(filter.getType()).equals("someAddType")&& Objects.requireNonNull(filter.getSubType()).equals("someAddSubType"))
                return new PageImpl<>(addtionalTypeApiResponseList, pageable, addtionalTypeApiResponseList.size());
            else
                return new PageImpl<>(rootTypeApiResponseList, pageable, rootTypeApiResponseList.size());
        });

        final var caseNotes = caseNoteService.getCaseNotes(offenderIdentifier, rootTypeFilter, pageable).getContent();

        assertThat(caseNotes.size()).isEqualTo(3);
        assertThat(caseNotes.stream().filter(x->x.getType().equals("someType")&&x.getSubType().equals("someSubType")).count()).isEqualTo(2);
        assertThat(caseNotes.stream().filter(x->x.getType().equals("someAddType")&&x.getSubType().equals("someAddSubType")).count()).isEqualTo(1);
    }

    @Test
    public void getCaseNoteWithAmendment_callElite2() {
        final var nomisCaseNote = createNomisCaseNote();
        nomisCaseNote.setAmendments(
            List.of(
                NomisCaseNoteAmendment.builder()
                    .additionalNoteText("additional details")
                    .authorUsername("AAA11B")
                    .creationDateTime(LocalDateTime.parse("2019-03-24T11:22"))
                    .build()
            )
        );
        when(externalApiService.getOffenderCaseNote(anyString(), anyLong())).thenReturn(nomisCaseNote);

        final var caseNote = caseNoteService.getCaseNote("12345", "21455");

        assertThat(caseNote).isEqualToIgnoringGivenFields(nomisCaseNote, "authorUsername", "locationId", "text", "caseNoteId", "authorUserId", "eventId", "sensitive", "amendments");
        assertThat(caseNote.getText()).isEqualTo("original");
        assertThat(caseNote.getAuthorUserId()).isEqualTo("23456");
        assertThat(caseNote.getLocationId()).isEqualTo("agency");
        assertThat(caseNote.getCaseNoteId()).isEqualTo("12345");
        assertThat(caseNote.getEventId()).isEqualTo(12345);
        assertThat(caseNote.getAmendments().get(0).getAdditionalNoteText()).isEqualTo("additional details");
        assertThat(caseNote.getAmendments().get(0).getAuthorUserName()).isEqualTo("AAA11B");
        assertThat(caseNote.getAmendments().get(0).getCreationDateTime()).isEqualTo("2019-03-24T11:22");
    }

    @Test
    public void amendCaseNote_callElite2() {
        final var nomisCaseNote = createNomisCaseNote();
        when(externalApiService.amendOffenderCaseNote(anyString(), anyLong(), any())).thenReturn(nomisCaseNote);

        final var caseNote = caseNoteService.amendCaseNote("12345", "21455", new UpdateCaseNote("text"));

        assertThat(caseNote).isEqualToIgnoringGivenFields(nomisCaseNote, "authorUsername", "locationId", "text", "caseNoteId", "authorUserId", "eventId", "sensitive");
        assertThat(caseNote.getText()).isEqualTo("original");
        assertThat(caseNote.getAuthorUserId()).isEqualTo("23456");
        assertThat(caseNote.getLocationId()).isEqualTo("agency");
        assertThat(caseNote.getCaseNoteId()).isEqualTo("12345");
        assertThat(caseNote.getEventId()).isEqualTo(12345);
    }

    @Test
    public void amendCaseNote_noAddRole() {
        final var noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        when(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote));

        assertThatThrownBy(() -> caseNoteService.amendCaseNote("12345", UUID.randomUUID().toString(), new UpdateCaseNote("text"))).isInstanceOf(AccessDeniedException.class);

        verify(securityUserContext).isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES");
    }

    @Test
    public void amendCaseNote_notFound() {
        final var caseNoteIdentifier = UUID.randomUUID().toString();

        assertThatThrownBy(() -> caseNoteService.amendCaseNote("12345", caseNoteIdentifier, new UpdateCaseNote("text")))
                .isInstanceOf(EntityNotFoundException.class).hasMessage(String.format("Resource with id [%s] not found.", caseNoteIdentifier));
    }

    @Test
    public void amendCaseNote_wrongOffender() {
        final var noteType = CaseNoteType.builder().type("sometype").restrictedUse(false).parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        when(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote));

        assertThatThrownBy(() -> caseNoteService.amendCaseNote("12345", UUID.randomUUID().toString(), new UpdateCaseNote("text")))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("Resource with id [12345] not found.");
    }

    @Test
    public void deleteOffenderTest() {
        when(repository.deleteOffenderCaseNoteByOffenderIdentifier(eq("A1234AC"))).thenReturn(3);
        final var offendersDeleted = caseNoteService.deleteCaseNotesForOffender("A1234AC");
        assertThat(offendersDeleted).isEqualTo(3);
    }

    @Test
    public void deleteOffenderTest_telemetry() {
        when(repository.deleteOffenderCaseNoteByOffenderIdentifier(eq("A1234AC"))).thenReturn(3);
        caseNoteService.deleteCaseNotesForOffender("A1234AC");
        verify(telemetryClient).trackEvent("OffenderDelete", Map.of("offenderNo", "A1234AC", "count", "3"), null);
    }

    @Test
    public void amendCaseNote() {
        final var noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        when(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote));
        when(securityUserContext.isOverrideRole(anyString(), anyString())).thenReturn(Boolean.TRUE);
        when(securityUserContext.getCurrentUser()).thenReturn(new UserIdUser("user", "userId"));
        when(externalApiService.getUserFullName(anyString())).thenReturn("author");

        final var caseNote = caseNoteService.amendCaseNote("A1234AC", UUID.randomUUID().toString(), new UpdateCaseNote("text"));
        assertThat(caseNote.getAmendments()).hasSize(1);
        final var expected = CaseNoteAmendment.builder()
                .additionalNoteText("text")
                .authorName("author")
                .authorUserId("some id")
                .authorUserName("user")
                .build();
        assertThat(caseNote.getAmendments().get(0)).isEqualToComparingOnlyGivenFields(expected, "additionalNoteText", "authorName", "authorUserName");
    }

    @Test
    public void softDeleteCaseNote() {
        final var noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        final var offenderCaseNoteId = offenderCaseNote.getId();
        when(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote));
        when(securityUserContext.getCurrentUser()).thenReturn(new UserIdUser("user", "userId"));

        caseNoteService.softDeleteCaseNote("A1234AC", offenderCaseNoteId.toString());

        verify(repository).deleteById(offenderCaseNoteId);
    }

    @Test
    public void softDeleteCaseNote_telemetry() {
        final var noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        final var offenderCaseNoteId = offenderCaseNote.getId();
        when(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote));
        when(securityUserContext.getCurrentUser()).thenReturn(new UserIdUser("user", "userId"));

        caseNoteService.softDeleteCaseNote("A1234AC", offenderCaseNoteId.toString());

        verify(telemetryClient).trackEvent("SecureCaseNoteSoftDelete", Map.of("userName", "user", "offenderId", "A1234AC", "case note id", offenderCaseNoteId.toString()), null);
    }

    @Test
    public void softDeleteCaseNoteEntityNotFoundExceptionThrownWhenCaseNoteNotFound() {

        assertThatThrownBy(() -> caseNoteService.softDeleteCaseNote("A1234AC", UUID.randomUUID().toString())).isInstanceOf(EntityNotFoundException.class);

    }

    @Test
    public void softDeleteCaseNoteEntityNotFoundExceptionThrownWhenCaseNoteDoesntBelongToOffender() {
        final var noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        final var offenderCaseNoteId = offenderCaseNote.getId();
        when(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote));

        assertThatThrownBy(() -> caseNoteService.softDeleteCaseNote("Z9999ZZ", offenderCaseNoteId.toString())).isInstanceOf(ValidationException.class);
    }

    @Test
    public void softDeleteCaseNoteAmendment() {
        final var noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNoteAmendment = createOffenderCaseNoteAmendment(noteType);
        when(amendmentRepository.findById(1L)).thenReturn(offenderCaseNoteAmendment);
        when(securityUserContext.getCurrentUser()).thenReturn(new UserIdUser("user", "userId"));

        caseNoteService.softDeleteCaseNoteAmendment("A1234AC", 1L);

        verify(amendmentRepository).deleteById(1L);
    }

    @Test
    public void softDeleteCaseNoteAmendment_telemetry() {
        final var noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNoteAmendment = createOffenderCaseNoteAmendment(noteType);
        when(amendmentRepository.findById(1L)).thenReturn(offenderCaseNoteAmendment);
        when(securityUserContext.getCurrentUser()).thenReturn(new UserIdUser("user", "userId"));

        caseNoteService.softDeleteCaseNoteAmendment("A1234AC", 1L);

        verify(telemetryClient).trackEvent("SecureCaseNoteAmendmentSoftDelete", Map.of("userName", "user", "offenderId", "A1234AC", "case note amendment id", "1"), null);
    }

    @Test
    public void softDeleteCaseNoteAmendmentEntityNotFoundExceptionThrownWhenCaseNoteNotFound() {

        assertThatThrownBy(() -> caseNoteService.softDeleteCaseNoteAmendment("A1234AC", 1L)).isInstanceOf(EntityNotFoundException.class);

    }

    @Test
    public void softDeleteCaseNoteAmendmentEntityNotFoundExceptionThrownWhenCaseNoteDoesntBelongToOffender() {
        final var noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNoteAmendment = createOffenderCaseNoteAmendment(noteType);
        when(amendmentRepository.findById(any())).thenReturn(offenderCaseNoteAmendment);

        assertThatThrownBy(() -> caseNoteService.softDeleteCaseNoteAmendment("Z9999ZZ", 1L)).isInstanceOf(ValidationException.class);
    }


    private NomisCaseNote createNomisCaseNote() {

        return NomisCaseNote.builder()
                .agencyId("agency")
                .authorName("somebody")
                .caseNoteId(12345)
                .creationDateTime(LocalDateTime.parse("2019-03-23T11:22"))
                .occurrenceDateTime(LocalDateTime.parse("2019-04-16T10:42"))
                .originalNoteText("original")
                .source("WHERE")
                .staffId(23456L)
                .subType("SUB")
                .subTypeDescription("Sub desc")
                .text("new text")
                .type("type")
                .typeDescription("Type desc")
                .offenderIdentifier("12345")
                .build();
    }

    private NomisCaseNote createNomisCaseNote(String type, String subType) {

        return NomisCaseNote.builder()
            .agencyId("agency")
            .authorName("somebody")
            .caseNoteId(12345)
            .creationDateTime(LocalDateTime.parse("2019-03-23T11:22"))
            .occurrenceDateTime(LocalDateTime.parse("2019-04-16T10:42"))
            .originalNoteText("original")
            .source("WHERE")
            .staffId(23456L)
            .subType(subType)
            .subTypeDescription("Sub desc")
            .text("new text")
            .type(type)
            .typeDescription("Type desc")
            .offenderIdentifier("12345")
            .build();
    }

    private OffenderCaseNote createOffenderCaseNote(final CaseNoteType caseNoteType) {
        return OffenderCaseNote.builder()
                .id(UUID.randomUUID())
                .occurrenceDateTime(now())
                .locationId("MDI")
                .authorUsername("USER2")
                .authorUserId("some user")
                .authorName("Mickey Mouse")
                .offenderIdentifier("A1234AC")
                .caseNoteType(caseNoteType)
                .noteText("HELLO")
                .build();
    }

    private Optional<OffenderCaseNoteAmendment> createOffenderCaseNoteAmendment(final CaseNoteType caseNoteType) {
        return Optional.of(OffenderCaseNoteAmendment
                .builder()
                .caseNote(createOffenderCaseNote(caseNoteType))
                .id(1L)
                .noteText("A")
                .authorName("some user")
                .build());
    }

}


