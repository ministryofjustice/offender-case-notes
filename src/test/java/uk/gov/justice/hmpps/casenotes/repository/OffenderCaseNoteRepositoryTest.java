package uk.gov.justice.hmpps.casenotes.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote.OffenderCaseNoteBuilder;
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType;

import java.util.Set;

import static java.time.LocalDateTime.now;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SensitiveCaseNoteType genType;

    @Before
    public void setUp() {
        genType = caseNoteTypeRepository.findSensitiveCaseNoteTypeByParentType_TypeAndType(PARENT_TYPE, SUB_TYPE);
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

        caseNote.addAmendment("Another Note 0", "someuser", "Some User", "user id");
        assertThat(caseNote.getAmendments()).hasSize(1);

        final var persistedEntity = repository.save(caseNote);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getId()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(persistedEntity.getId()).orElseThrow();

        retrievedEntity.addAmendment("Another Note 1", "someuser", "Some User", "user id");
        retrievedEntity.addAmendment("Another Note 2", "someuser", "Some User", "user id");

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        final var retrievedEntity2 = repository.findById(persistedEntity.getId()).orElseThrow();

        assertThat(retrievedEntity2.getAmendments()).hasSize(3);

        assertThat(retrievedEntity2.getAmendment(1).orElseThrow().getNoteText()).isEqualTo("Another Note 0");
        final var offenderCaseNoteAmendment3 = retrievedEntity2.getAmendment(3).orElseThrow();
        assertThat(offenderCaseNoteAmendment3.getNoteText()).isEqualTo("Another Note 2");

        retrievedEntity2.addAmendment("Another Note 3", "USER1", "Mickey Mouse", "user id");

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        final var retrievedEntity3 = repository.findById(persistedEntity.getId()).orElseThrow();

        assertThat(retrievedEntity3.getAmendments()).hasSize(4);

        assertThat(retrievedEntity3.getAmendment(4).orElseThrow().getNoteText()).isEqualTo("Another Note 3");
    }

    @Test
    public void testOffenderCaseNoteFilter() {
        final var entity = OffenderCaseNote.builder()
                .occurrenceDateTime(now())
                .locationId("BOB")
                .authorUsername("FILTER")
                .authorUserId("some id")
                .authorName("Mickey Mouse")
                .offenderIdentifier("A1234BD")
                .sensitiveCaseNoteType(genType)
                .noteText("HELLO")
                .build();
        repository.save(entity);

        final var allCaseNotes = repository.findAll(OffenderCaseNoteFilter.builder()
                .type(" ").subType(" ").authorUsername(" ").locationId(" ").offenderIdentifier(" ").build());
        assertThat(allCaseNotes.size()).isGreaterThan(0);

        final var caseNotes = repository.findAll(OffenderCaseNoteFilter.builder()
                .type(PARENT_TYPE).subType(SUB_TYPE).authorUsername("FILTER").locationId("BOB").offenderIdentifier("A1234BD").build());
        assertThat(caseNotes).hasSize(1);
    }

    @Test
    public void testAmendmentUpdatesCaseNoteModification() {
        final var twoDaysAgo = now().minusDays(2);

        final var noteText = "updates old note";
        final var oldNote = repository.save(transientEntityBuilder().noteText(noteText).build());

        final var noteTextWithAmendment = "updates old note with old amendment";
        final var oldNoteWithOldAmendment = repository.save(transientEntityBuilder().noteText(noteTextWithAmendment).build());
        oldNoteWithOldAmendment.addAmendment("Some amendment", "someuser", "Some User", "user id");
        repository.save(oldNoteWithOldAmendment);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        // set the notes to two days ago
        final var update = jdbcTemplate.update("update offender_case_note set modify_date_time = ? where offender_case_note_id in (?, ?)", twoDaysAgo,
                oldNote.getId().toString(), oldNoteWithOldAmendment.getId().toString());
        assertThat(update).isEqualTo(2);

        // now add an amendment
        final var retrievedOldNote = repository.findById(oldNote.getId()).orElseThrow();
        retrievedOldNote.addAmendment("An amendment", "anotheruser", "Another User", "user id");
        repository.save(retrievedOldNote);

        final var yesterday = now().minusDays(1);
        final var rows = repository.findBySensitiveCaseNoteType_ParentType_TypeInAndModifyDateTimeAfterOrderByModifyDateTime(Set.of("POM"), yesterday, Pageable.unpaged());
        assertThat(rows).extracting(OffenderCaseNote::getNoteText).contains(noteText).doesNotContain(noteTextWithAmendment);
    }


    @Test
    public void findByModifiedDate() {
        final var twoDaysAgo = now().minusDays(2);

        final var oldNoteText = "old note";
        final var oldNote = repository.save(transientEntityBuilder().noteText(oldNoteText).build());

        final var newNoteText = "new note";
        repository.save(transientEntityBuilder().noteText(newNoteText).build());

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // set the old notes two days ago so won't be returned
        final var update = jdbcTemplate.update("update offender_case_note set modify_date_time = ? where offender_case_note_id in (?)", twoDaysAgo, oldNote.getId().toString());
        assertThat(update).isEqualTo(1);

        final var yesterday = now().minusDays(1);
        final var rows = repository.findBySensitiveCaseNoteType_ParentType_TypeInAndModifyDateTimeAfterOrderByModifyDateTime(Set.of("POM", "BOB"), yesterday, Pageable.unpaged());
        assertThat(rows).extracting(OffenderCaseNote::getNoteText).contains(newNoteText).doesNotContain(oldNoteText);
    }

    @Test
    public void testGenerationOfEventId() {
        final var note = repository.save(transientEntity());

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(repository.findById(note.getId()).orElseThrow().getEventId()).isLessThan(0);
    }

    private OffenderCaseNote transientEntity() {
        return transientEntityBuilder().build();
    }

    private OffenderCaseNoteBuilder transientEntityBuilder() {
        return OffenderCaseNote.builder()
                .occurrenceDateTime(now())
                .locationId("MDI")
                .authorUsername("USER2")
                .authorUserId("some id")
                .authorName("Mickey Mouse")
                .offenderIdentifier("A1234BD")
                .sensitiveCaseNoteType(genType)
                .noteText("HELLO");
    }
}
