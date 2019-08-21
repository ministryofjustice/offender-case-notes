package uk.gov.justice.hmpps.casenotes.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType;
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType;
import uk.gov.justice.hmpps.casenotes.repository.CaseNoteTypeRepository;
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository;
import uk.gov.justice.hmpps.casenotes.repository.ParentCaseNoteTypeRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private CaseNoteService caseNoteService;

    @Before
    public void setUp() {
        caseNoteService = new CaseNoteService(repository, caseNoteTypeRepository, parentCaseNoteTypeRepository, securityUserContext, externalApiService);
    }

    @Test
    public void createCaseNote_callElite2() {
        when(parentCaseNoteTypeRepository.findById(anyString())).thenReturn(Optional.empty());

        final var nomisCaseNote = createNomisCaseNote();
        when(externalApiService.createCaseNote(anyString(), any())).thenReturn(nomisCaseNote);

        final var caseNote = caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").build());

        assertThat(caseNote).isEqualToIgnoringGivenFields(nomisCaseNote, "authorUsername", "locationId", "text", "caseNoteId");
        assertThat(caseNote.getText()).isEqualTo("original");
        assertThat(caseNote.getAuthorUsername()).isEqualTo("23456");
        assertThat(caseNote.getLocationId()).isEqualTo("agency");
        assertThat(caseNote.getCaseNoteId()).isEqualTo("12345");
        verify(parentCaseNoteTypeRepository).findById("type");
    }

    @Test
    public void createCaseNote_noAddRole() {
        when(parentCaseNoteTypeRepository.findById(anyString())).thenReturn(Optional.of(ParentNoteType.builder().build()));
        when(securityUserContext.isOverrideRole(anyString())).thenReturn(Boolean.FALSE);

        assertThatThrownBy(() -> caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").build())).isInstanceOf(AccessDeniedException.class);

        verify(securityUserContext).isOverrideRole("ADD_SENSITIVE_CASE_NOTES");
    }

    @Test
    public void createCaseNote_caseNoteTypeNotFound() {
        when(parentCaseNoteTypeRepository.findById(anyString())).thenReturn(Optional.of(ParentNoteType.builder().build()));
        when(securityUserContext.isOverrideRole(anyString())).thenReturn(Boolean.TRUE);

        assertThatThrownBy(() -> caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").build())).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    public void createCaseNote() {
        when(parentCaseNoteTypeRepository.findById(anyString())).thenReturn(Optional.of(ParentNoteType.builder().build()));
        when(securityUserContext.isOverrideRole(anyString())).thenReturn(Boolean.TRUE);
        final var noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build();
        when(caseNoteTypeRepository.findCaseNoteTypeByParentTypeAndType(any(), anyString())).thenReturn(noteType);
        final var offenderCaseNote = createOffenderCaseNote(noteType);
        when(repository.save(any())).thenReturn(offenderCaseNote);

        final var createdNote = caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("sub").build());
        assertThat(createdNote).isEqualToIgnoringGivenFields(offenderCaseNote,
                "caseNoteId", "type", "typeDescription", "subType", "subTypeDescription", "source", "creationDateTime", "authorUsername", "authorName", "text");
        assertThat(createdNote.getText()).isEqualTo("HELLO");
    }

    private NomisCaseNote createNomisCaseNote() {
        return NomisCaseNote.builder()
                .agencyId("agency")
                .authorName("somebody")
                .caseNoteId(12345L)
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
                .occurrenceDateTime(LocalDateTime.now())
                .locationId("MDI")
                .staffUsername("USER2")
                .staffName("Mickey Mouse")
                .offenderIdentifier("A1234AC")
                .sensitiveCaseNoteType(caseNoteType)
                .noteText("HELLO")
                .build();
    }
}
