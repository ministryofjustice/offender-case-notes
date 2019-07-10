package uk.gov.justice.hmpps.casenotes.repository;

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
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNoteAmendment;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
public class OffenderCaseNoteRepositoryTest {

    @Autowired
    private OffenderCaseNoteRepository repository;

    @Test
    @WithAnonymousUser
    public void testPersistCaseNote() {

        var caseNote = transientEntity();

        var persistedEntity = repository.save(caseNote);

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

        var caseNote = transientEntity();

        caseNote.addAmendment("Another Note 0");
        assertThat(caseNote.getAmendments()).hasSize(1);

        var persistedEntity = repository.save(caseNote);

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

        assertThat(retrievedEntity2.getAmendment(1).get().getNoteText()).isEqualTo("Another Note 0");
        OffenderCaseNoteAmendment offenderCaseNoteAmendment3 = retrievedEntity2.getAmendment(3).get();
        assertThat(offenderCaseNoteAmendment3.getNoteText()).isEqualTo("Another Note 2");

        retrievedEntity2.addAmendment("Another Note 3", "LEI", "USER1");

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        final var retrievedEntity3 = repository.findById(persistedEntity.getId()).orElseThrow();

        assertThat(retrievedEntity3.getAmendments()).hasSize(4);

        assertThat(retrievedEntity3.getAmendment(4).get().getNoteText()).isEqualTo("Another Note 3");

    }

    private OffenderCaseNote transientEntity() {
        return OffenderCaseNote.builder()
                .occurrenceDateTime(LocalDateTime.now())
                .locationId("MDI")
                .staffUsername("USER2")
                .offenderIdentifier("A1234AA")
                .type("XXXX")
                .subType("XXXX1")
                .noteText("HELLO")
                .build();
    }
}
