package uk.gov.justice.hmpps.casenotes.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.casenotes.config.AuthAwareAuthenticationToken;
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.health.IntegrationTest;
import uk.gov.justice.hmpps.casenotes.model.CaseNoteType;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote.OffenderCaseNoteBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class OffenderCaseNoteRepositoryTest extends IntegrationTest {

    private static final String PARENT_TYPE = "POM";
    private static final String SUB_TYPE = "GEN";
    public static final String OFFENDER_IDENTIFIER = "A1234BD";

    @Autowired
    private OffenderCaseNoteRepository repository;

    @Autowired
    private CaseNoteTypeRepository caseNoteTypeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CaseNoteType genType;

    @BeforeEach
    public void setUp() {
        final var jwt = Jwt.withTokenValue("some").subject("anonymous").header("head", "something").build();
        SecurityContextHolder.getContext()
            .setAuthentication(new AuthAwareAuthenticationToken(jwt, "userId", Collections.emptyList()));
        genType = caseNoteTypeRepository.findByParentTypeAndType(PARENT_TYPE, SUB_TYPE).orElseThrow();
    }

    @Test
    public void testPersistCaseNote() {

        final var caseNote = transientEntity(OFFENDER_IDENTIFIER);

        final var persistedEntity = repository.save(caseNote);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(persistedEntity.getId()).isNotNull();

        TestTransaction.start();

        final var retrievedEntity = repository.findById(persistedEntity.getId()).orElseThrow();

        assertThat(retrievedEntity).usingRecursiveComparison()
            .ignoringFields("occurrenceDateTime", "caseNoteType", "eventId", "createDateTime", "modifyDateTime", "legacyId")
            .isEqualTo(caseNote);
        assertThat(retrievedEntity.getOccurredAt()).isEqualToIgnoringNanos(caseNote.getOccurredAt());
        assertThat(retrievedEntity.getCaseNoteType()).isEqualTo(caseNote.getCaseNoteType());
    }

    @Test
    @WithAnonymousUser
    public void testPersistCaseNoteAndAmendment() {

        final var caseNote = transientEntity(OFFENDER_IDENTIFIER);

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

        assertThat(retrievedEntity2.getAmendments().first().getNoteText()).isEqualTo("Another Note 0");
        final var offenderCaseNoteAmendment3 = new ArrayList<>(retrievedEntity2.getAmendments()).get(2);
        assertThat(offenderCaseNoteAmendment3.getNoteText()).isEqualTo("Another Note 2");

        retrievedEntity2.addAmendment("Another Note 3", "USER1", "Mickey Mouse", "user id");

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        final var retrievedEntity3 = repository.findById(persistedEntity.getId()).orElseThrow();

        assertThat(retrievedEntity3.getAmendments()).hasSize(4);

        assertThat(retrievedEntity3.getAmendments().last().getNoteText()).isEqualTo("Another Note 3");
    }

    @Test
    public void testOffenderCaseNoteFilter() {
        final var entity = OffenderCaseNote.builder()
                .occurredAt(now())
                .locationId("BOB")
                .authorUsername("FILTER")
                .authorUserId("some id")
                .authorName("Mickey Mouse")
                .personIdentifier(OFFENDER_IDENTIFIER)
                .caseNoteType(genType)
                .noteText("HELLO")
                .build();
        repository.save(entity);

        final var allCaseNotes = repository.findAll(new OffenderCaseNoteFilter());
        assertThat(allCaseNotes.size()).isGreaterThan(0);

        final var caseNotes = repository.findAll(new OffenderCaseNoteFilter(OFFENDER_IDENTIFIER, "BOB", "FILTER", false, null, null, Map.of(PARENT_TYPE, Set.of(SUB_TYPE))));
        assertThat(caseNotes).hasSize(1);
    }

    @Test
    public void testGenerationOfEventId() {
        final var note = repository.save(transientEntity(OFFENDER_IDENTIFIER));

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(repository.findById(note.getId()).orElseThrow().getLegacyId()).isLessThan(0);
    }

    @Test
    public void testDeleteCaseNotes() {

        final var persistedEntity = repository.save(transientEntityBuilder("X1111XX").noteText("note to delete").build());

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var deletedCaseNotes = repository.deleteCaseNoteByPersonIdentifier("X1111XX");
        assertThat(deletedCaseNotes).isEqualTo(1);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        assertThat(repository.findById(persistedEntity.getId())).isEmpty();

        final var sql = String.format("select count(*) FROM case_note where id = '%s'", persistedEntity.getId().toString());
        final var caseNoteCountAfter = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(caseNoteCountAfter).isEqualTo(0);
    }

    @Test
    public void testDeleteOfSoftDeletedCaseNotes() {

        final var persistedEntity = repository.save(transientEntityBuilder("X2111XX").noteText("note to delete").build());

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        repository.delete(persistedEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var deletedCaseNotes = repository.deleteCaseNoteByPersonIdentifier("X2111XX");
        assertThat(deletedCaseNotes).isEqualTo(1);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(repository.findById(persistedEntity.getId())).isEmpty();
        TestTransaction.end();

        final var sql = String.format("select count(*) FROM case_note where id = '%s'", persistedEntity.getId().toString());
        final var caseNoteCountAfter = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(caseNoteCountAfter).isEqualTo(0);
    }

    @Test
    public void testDeleteOfSoftDeletedCaseNotesAmendments() {

        final var persistedEntity = repository.save(transientEntityBuilder("X3111XX")
                .noteText("note to delete")
                .build());
        persistedEntity.addAmendment("Another Note 0", "someuser", "Some User", "user id");
        repository.save(persistedEntity);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        repository.delete(persistedEntity);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        final var offenderAmendmentSql = """
            select count(1) from case_note_amendment a
            join case_note c on a.case_note_id = c.id
            where c.person_identifier = 'X3111XX'
            """;

        final var caseNoteCountBefore = jdbcTemplate.queryForObject(offenderAmendmentSql, Integer.class);
        assertThat(caseNoteCountBefore).isEqualTo(1);

        TestTransaction.start();
        repository.deleteCaseNoteAmendmentsByPersonIdentifier("X3111XX");
        final var deletedCaseNotes = repository.deleteCaseNoteByPersonIdentifier("X3111XX");
        assertThat(deletedCaseNotes).isEqualTo(1);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        assertThat(repository.findById(persistedEntity.getId())).isEmpty();
        TestTransaction.end();

        final var caseNoteCountAfter = jdbcTemplate.queryForObject(offenderAmendmentSql, Integer.class);
        assertThat(caseNoteCountAfter).isZero();

    }

    @Test
    @WithAnonymousUser
    public void testPersistCaseNoteAndAmendmentAndThenDelete() {

        final var caseNote = transientEntity(OFFENDER_IDENTIFIER);
        caseNote.addAmendment("Another Note 0", "someuser", "Some User", "user id");
        final var persistedEntity = repository.save(caseNote);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        final var retrievedEntity = repository.findById(persistedEntity.getId()).orElseThrow();

        retrievedEntity.addAmendment("Another Note 1", "someuser", "Some User", "user id");
        retrievedEntity.addAmendment("Another Note 2", "someuser", "Some User", "user id");

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        repository.deleteCaseNoteAmendmentsByPersonIdentifier(caseNote.getPersonIdentifier());
        final var deletedEntities = repository.deleteCaseNoteByPersonIdentifier(caseNote.getPersonIdentifier());

        assertThat(deletedEntities).isEqualTo(1);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var deletedEntity = repository.findById(persistedEntity.getId());
        assertThat(deletedEntity).isEmpty();
    }

    @Test
    @WithAnonymousUser
    public void testModifyOffenderIdentifier() {
        final var caseNote = transientEntity("A1234ZZ");
        caseNote.addAmendment("Another Note 0", "someuser", "Some User", "user id");
        final var persistedEntity = repository.save(caseNote);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedCaseNote = repository.findById(persistedEntity.getId()).orElseThrow();
        assertThat(retrievedCaseNote.getPersonIdentifier()).isEqualTo("A1234ZZ");

        TestTransaction.end();
        TestTransaction.start();

        final var rows = repository.updateOffenderIdentifier("A1234ZZ", OFFENDER_IDENTIFIER);

        assertThat(rows).isEqualTo(1);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var modifiedIdentity = repository.findById(persistedEntity.getId()).orElseThrow();
        assertThat(modifiedIdentity.getPersonIdentifier()).isEqualTo(OFFENDER_IDENTIFIER);
    }

    @Test
    public void testOffenderCaseNoteSoftDeleted() {
        final var caseNote = transientEntity("A2345AB");
        final var persistedEntity = repository.save(caseNote);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedCaseNote = repository.findById(persistedEntity.getId()).orElseThrow();
        repository.delete(retrievedCaseNote);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedSoftDeleteCaseNote = repository.findById(persistedEntity.getId());
        assertThat(retrievedSoftDeleteCaseNote).isEmpty();
    }


    @Test
    @WithAnonymousUser
    public void testRetrieveASoftDeletedFalseCaseNote() {

        final var persistedEntity = repository.save(transientEntityBuilder("X4111XX").noteText("note to retrieve").build());

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var caseNoteId = persistedEntity.getId();

        final var caseNote = repository.findById(caseNoteId).orElseThrow();
        assertThat(caseNote.getPersonIdentifier()).isEqualTo("X4111XX");

        TestTransaction.end();

        final var sql = String.format("select person_identifier from case_note where id = '%s'", persistedEntity.getId().toString());
        final var caseNoteOffenderIdentifierIgnoreSoftDelete = jdbcTemplate.queryForObject(sql, String.class);
        assertThat(caseNoteOffenderIdentifierIgnoreSoftDelete).isEqualTo("X4111XX");
    }


    private OffenderCaseNote transientEntity(final String offenderIdentifier) {
        return transientEntityBuilder(offenderIdentifier).build();
    }

    private OffenderCaseNoteBuilder transientEntityBuilder(final String offenderIdentifier) {
        return OffenderCaseNote.builder()
                .occurredAt(now())
                .locationId("MDI")
                .authorUsername("USER2")
                .authorUserId("some id")
                .authorName("Mickey Mouse")
                .personIdentifier(offenderIdentifier)
                .caseNoteType(genType)
                .noteText("HELLO")
                .createdBy("SYS")
                .systemGenerated(false);

    }
}
