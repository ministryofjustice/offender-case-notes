package uk.gov.justice.hmpps.casenotes.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.casenotes.config.AuthAwareAuthenticationToken;
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.health.BasicIntegrationTest;
import uk.gov.justice.hmpps.casenotes.model.CaseNoteSubType;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote.OffenderCaseNoteBuilder;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class OffenderCaseNoteRepositoryTest extends BasicIntegrationTest {

    private static final String PARENT_TYPE = "POM";
    private static final String SUB_TYPE = "GEN";
    public static final String OFFENDER_IDENTIFIER = "A1234BD";

    @Autowired
    private OffenderCaseNoteRepository repository;

    @Autowired
    private CaseNoteSubTypeRepository caseNoteSubTypeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CaseNoteSubType genType;

    @BeforeEach
    public void setUp() {
        final var jwt = Jwt.withTokenValue("some").subject("anonymous").header("head", "something").build();
        SecurityContextHolder.getContext()
            .setAuthentication(new AuthAwareAuthenticationToken(jwt, "userId", Collections.emptyList()));
        genType = caseNoteSubTypeRepository.findByParentTypeAndType(PARENT_TYPE, SUB_TYPE).orElseThrow();
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
                .subType(genType)
                .text("HELLO")
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

        assertThat(repository.findById(note.getId()).orElseThrow().getLegacyId()).isNegative();
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
                .subType(genType)
                .text("HELLO")
                .createdBy("SYS")
                .systemGenerated(false);

    }
}
