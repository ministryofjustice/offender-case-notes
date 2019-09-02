package uk.gov.justice.hmpps.casenotes.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@WithAnonymousUser
public class OffenderCaseNoteRepositoryTest {

    private static final String PARENT_TYPE = "POM";
    private static final String SUB_TYPE = "GEN";

    @Autowired
    private OffenderCaseNoteRepository repository;

    @Autowired
    private CaseNoteTypeRepository caseNoteTypeRepository;

    private SensitiveCaseNoteType sampleType;

    @Before
    public void setUp() {
        sampleType = caseNoteTypeRepository.findSensitiveCaseNoteTypeByParentType_TypeAndType(PARENT_TYPE, SUB_TYPE);
    }

    @Test
    public void testPersistCaseNote() {

        final var caseNote = transientEntity();

        final var persistedEntity = repository.save(caseNote);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getId()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(persistedEntity.getId()).orElseThrow();

        // equals only compares the business key columns
        assertThat(retrievedEntity).isEqualTo(caseNote);

        assertThat(retrievedEntity.getCreateUserId()).isEqualTo("anonymous");
    }

    @Test
    @WithAnonymousUser
    public void testPersistCaseNoteAndAmendment() {

        final var caseNote = transientEntity();

        caseNote.addAmendment("Another Note 0");
        assertThat(caseNote.getAmendments()).hasSize(1);

        final var persistedEntity = repository.save(caseNote);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getId()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(persistedEntity.getId()).orElseThrow();

        retrievedEntity.addAmendment("Another Note 1");
        retrievedEntity.addAmendment("Another Note 2");

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        final var retrievedEntity2 = repository.findById(persistedEntity.getId()).orElseThrow();

        assertThat(retrievedEntity2.getAmendments()).hasSize(3);

        assertThat(retrievedEntity2.getAmendment(1).orElseThrow().getNoteText()).isEqualTo("Another Note 0");
        final var offenderCaseNoteAmendment3 = retrievedEntity2.getAmendment(3).orElseThrow();
        assertThat(offenderCaseNoteAmendment3.getNoteText()).isEqualTo("Another Note 2");

        retrievedEntity2.addAmendment("Another Note 3", "USER1", "Mickey Mouse");

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        final var retrievedEntity3 = repository.findById(persistedEntity.getId()).orElseThrow();

        assertThat(retrievedEntity3.getAmendments()).hasSize(4);

        assertThat(retrievedEntity3.getAmendment(4).orElseThrow().getNoteText()).isEqualTo("Another Note 3");

    }

    private OffenderCaseNote transientEntity() {
        return OffenderCaseNote.builder()
                .occurrenceDateTime(LocalDateTime.now())
                .locationId("MDI")
                .authorUsername("USER2")
                .authorName("Mickey Mouse")
                .offenderIdentifier("A1234BD")
                .sensitiveCaseNoteType(sampleType)
                .noteText("HELLO")
                .build();
    }
}
