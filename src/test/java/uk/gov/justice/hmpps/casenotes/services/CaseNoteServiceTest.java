package uk.gov.justice.hmpps.casenotes.services;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;
import uk.gov.justice.hmpps.casenotes.config.UserIdAuthenticationConverter.UserIdUser;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteAmendment;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNote;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType;
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType;
import uk.gov.justice.hmpps.casenotes.repository.CaseNoteTypeRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;
import uk.gov.justice.hmpps.casenotes.repository.ParentCaseNoteTypeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CaseNoteServiceTest {
    @Mock
    private OffenderCaseNoteRepository repository;
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

    @Before
    public void setUp() {
        caseNoteService = new CaseNoteService(repository, caseNoteTypeRepository, parentCaseNoteTypeRepository, securityUserContext, externalApiService, caseNoteTypeMerger, telemetryClient);
    }

    @Test
    public void createCaseNote_callElite2() {
        when(caseNoteTypeRepository.findSensitiveCaseNoteTypeByParentType_TypeAndType(anyString(), anyString())).thenReturn(null);

        final var nomisCaseNote = createNomisCaseNote();
        when(externalApiService.createCaseNote(anyString(), any())).thenReturn(nomisCaseNote);

        final var caseNote = caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("SUB").build());

        assertThat(caseNote).isEqualToIgnoringGivenFields(nomisCaseNote, "authorUsername", "locationId", "text", "caseNoteId", "authorUserId", "eventId");
        assertThat(caseNote.getText()).isEqualTo("original");
        assertThat(caseNote.getAuthorUserId()).isEqualTo("23456");
        assertThat(caseNote.getLocationId()).isEqualTo("agency");
        assertThat(caseNote.getCaseNoteId()).isEqualTo("12345");
        assertThat(caseNote.getEventId()).isEqualTo(12345);
        verify(caseNoteTypeRepository).findSensitiveCaseNoteTypeByParentType_TypeAndType("type", "SUB");
    }

    @Test
    public void createCaseNote_noAddRole() {
        when(caseNoteTypeRepository.findSensitiveCaseNoteTypeByParentType_TypeAndType(anyString(), anyString())).thenReturn(SensitiveCaseNoteType.builder().build());
        when(securityUserContext.isOverrideRole(anyString(), anyString())).thenReturn(Boolean.FALSE);

        assertThatThrownBy(() -> caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("SUB").build())).isInstanceOf(AccessDeniedException.class);

        verify(securityUserContext).isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES");
    }

    @Test
    public void createCaseNote() {
        final var noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        when(caseNoteTypeRepository.findSensitiveCaseNoteTypeByParentType_TypeAndType(anyString(), anyString())).thenReturn(noteType);
        when(securityUserContext.isOverrideRole(anyString(), anyString())).thenReturn(Boolean.TRUE);
        when(securityUserContext.getCurrentUser()).thenReturn(new UserIdUser("someuser", "N/A", List.of(), "some id"));
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        when(repository.save(any())).thenReturn(offenderCaseNote);

        final var createdNote = caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("sub").build());
        assertThat(createdNote).isEqualToIgnoringGivenFields(offenderCaseNote,
                "caseNoteId", "type", "typeDescription", "subType", "subTypeDescription", "source", "creationDateTime", "text");
        assertThat(createdNote.getText()).isEqualTo("HELLO");
    }

    @Test
    public void getCaseNote_noAddRole() {
        assertThatThrownBy(() -> caseNoteService.getCaseNote("12345", UUID.randomUUID().toString())).isInstanceOf(AccessDeniedException.class);

        verify(securityUserContext).isOverrideRole("POM", "VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES");
    }

    @Test
    public void getCaseNote_notFound() {
        when(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(Boolean.TRUE);

        assertThatThrownBy(() -> caseNoteService.getCaseNote("12345", UUID.randomUUID().toString())).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    public void getCaseNote() {
        final var noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        when(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote));
        when(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(Boolean.TRUE);

        final var caseNote = caseNoteService.getCaseNote("12345", UUID.randomUUID().toString());
        assertThat(caseNote).isEqualToIgnoringGivenFields(offenderCaseNote,
                "caseNoteId", "type", "typeDescription", "subType", "subTypeDescription", "source", "creationDateTime", "authorUsername", "authorName", "text");
        assertThat(caseNote.getText()).isEqualTo("HELLO");
    }

    @Test
    public void getCaseNote_callElite2() {
        final var nomisCaseNote = createNomisCaseNote();
        when(externalApiService.getOffenderCaseNote(anyString(), anyLong())).thenReturn(nomisCaseNote);

        final var caseNote = caseNoteService.getCaseNote("12345", "21455");

        assertThat(caseNote).isEqualToIgnoringGivenFields(nomisCaseNote, "authorUsername", "locationId", "text", "caseNoteId", "authorUserId", "eventId");
        assertThat(caseNote.getText()).isEqualTo("original");
        assertThat(caseNote.getAuthorUserId()).isEqualTo("23456");
        assertThat(caseNote.getLocationId()).isEqualTo("agency");
        assertThat(caseNote.getCaseNoteId()).isEqualTo("12345");
        assertThat(caseNote.getEventId()).isEqualTo(12345);
    }

    @Test
    public void amendCaseNote_callElite2() {
        final var nomisCaseNote = createNomisCaseNote();
        when(externalApiService.amendOffenderCaseNote(anyString(), anyLong(), any())).thenReturn(nomisCaseNote);

        final var caseNote = caseNoteService.amendCaseNote("12345", "21455", new UpdateCaseNote("text"));

        assertThat(caseNote).isEqualToIgnoringGivenFields(nomisCaseNote, "authorUsername", "locationId", "text", "caseNoteId", "authorUserId", "eventId");
        assertThat(caseNote.getText()).isEqualTo("original");
        assertThat(caseNote.getAuthorUserId()).isEqualTo("23456");
        assertThat(caseNote.getLocationId()).isEqualTo("agency");
        assertThat(caseNote.getCaseNoteId()).isEqualTo("12345");
        assertThat(caseNote.getEventId()).isEqualTo(12345);
    }

    @Test
    public void amendCaseNote_noAddRole() {
        assertThatThrownBy(() -> caseNoteService.amendCaseNote("12345", UUID.randomUUID().toString(), new UpdateCaseNote("text"))).isInstanceOf(AccessDeniedException.class);

        verify(securityUserContext).isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES");
    }

    @Test
    public void amendCaseNote_notFound() {
        when(securityUserContext.isOverrideRole(anyString(), anyString())).thenReturn(Boolean.TRUE);
        final var caseNoteIdentifier = UUID.randomUUID().toString();

        assertThatThrownBy(() -> caseNoteService.amendCaseNote("12345", caseNoteIdentifier, new UpdateCaseNote("text")))
                .isInstanceOf(EntityNotFoundException.class).hasMessage(String.format("Resource with id [%s] not found.", caseNoteIdentifier));
    }

    @Test
    public void amendCaseNote_wrongOffender() {
        final var noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        when(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote));
        when(securityUserContext.isOverrideRole(anyString(), anyString())).thenReturn(Boolean.TRUE);

        assertThatThrownBy(() -> caseNoteService.amendCaseNote("12345", UUID.randomUUID().toString(), new UpdateCaseNote("text")))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("Resource with id [12345] not found.");
    }

    @Test
    public void deleteOffenderTest() {
        when(repository.deleteOffenderCaseNoteByOffenderIdentifier(eq("A1234AC"))).thenReturn(List.of(
                OffenderCaseNote.builder().offenderIdentifier("A1234AC").noteText("Test 1").build(),
                OffenderCaseNote.builder().offenderIdentifier("A1234AC").noteText("Test 2").build(),
                OffenderCaseNote.builder().offenderIdentifier("A1234AC").noteText("Test 3").build()
        ));
        final var offendersDeleted = caseNoteService.deleteCaseNotesForOffender("A1234AC");
        assertThat(offendersDeleted).isEqualTo(3);
    }

    @Test
    public void deleteOffenderTest_telemetry() {
        when(repository.deleteOffenderCaseNoteByOffenderIdentifier(eq("A1234AC"))).thenReturn(List.of(
                OffenderCaseNote.builder().offenderIdentifier("A1234AC").noteText("Test 1").build(),
                OffenderCaseNote.builder().offenderIdentifier("A1234AC").noteText("Test 2").build(),
                OffenderCaseNote.builder().offenderIdentifier("A1234AC").noteText("Test 3").build()
        ));
        caseNoteService.deleteCaseNotesForOffender("A1234AC");
        verify(telemetryClient).trackEvent("OffenderDelete", Map.of("offenderNo", "A1234AC", "count", "3"), null);
    }

    @Test
    public void amendCaseNote() {
        final var noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        when(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote));
        when(securityUserContext.isOverrideRole(anyString(), anyString())).thenReturn(Boolean.TRUE);
        when(securityUserContext.getCurrentUser()).thenReturn(new UserIdUser("user", "N/A", List.of(), "userId"));
        when(externalApiService.getUserFullName(anyString())).thenReturn("author");

        final var caseNote = caseNoteService.amendCaseNote("A1234AC", UUID.randomUUID().toString(), new UpdateCaseNote("text"));
        assertThat(caseNote.getAmendments()).hasSize(1);
        final var expected = CaseNoteAmendment.builder()
                .additionalNoteText("text")
                .authorName("author")
                .authorUserId("some id")
                .authorUserName("user")
                .sequence(1).build();
        assertThat(caseNote.getAmendments().get(0)).isEqualToComparingOnlyGivenFields(expected, "additionalNoteText", "authorName", "authorUserName", "sequence");
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

    private OffenderCaseNote createOffenderCaseNote(final SensitiveCaseNoteType caseNoteType) {
        return OffenderCaseNote.builder()
                .id(UUID.randomUUID())
                .occurrenceDateTime(now())
                .locationId("MDI")
                .authorUsername("USER2")
                .authorUserId("some user")
                .authorName("Mickey Mouse")
                .offenderIdentifier("A1234AC")
                .sensitiveCaseNoteType(caseNoteType)
                .noteText("HELLO")
                .build();
    }
}
