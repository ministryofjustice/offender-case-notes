package uk.gov.justice.hmpps.casenotes.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.casenotes.config.AuthAwareAuthenticationToken;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote.OffenderCaseNoteBuilder;
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType;

import java.util.Collections;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
public class OffenderCaseNoteAmendmentRepositoryTest {

    private static final String PARENT_TYPE = "POM";
    private static final String SUB_TYPE = "GEN";
    public static final String OFFENDER_IDENTIFIER = "A1234BD";

    @Autowired
    private OffenderCaseNoteRepository repository;

    @Autowired
    private OffenderCaseNoteAmendmentRepository amendmentRepository;

    @Autowired
    private CaseNoteTypeRepository caseNoteTypeRepository;

    private SensitiveCaseNoteType genType;

    @BeforeEach
    public void setUp() {
        final var jwt = Jwt.withTokenValue("some").subject("anonymous").header("head", "something").build();
        SecurityContextHolder.getContext().setAuthentication(
                new AuthAwareAuthenticationToken(jwt, "userId", Collections.emptyList()));
        genType = caseNoteTypeRepository.findSensitiveCaseNoteTypeByParentType_TypeAndType(PARENT_TYPE, SUB_TYPE);
    }

    @Test
    @WithAnonymousUser
    public void testOffenderCaseNoteAmendmentSoftDeleted() {
        final var caseNote = transientEntity("A2345BB");
        caseNote.addAmendment("Another Note 0", "someuser", "Some User", "user id");
        final var persistedEntity = repository.save(caseNote);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        final var amendmentId = repository.findById(persistedEntity.getId()).orElseThrow().getAmendment(1).get().getId();

        TestTransaction.end();
        TestTransaction.start();
        amendmentRepository.deleteById(amendmentId);
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedCaseNote = repository.findById(persistedEntity.getId()).orElseThrow();

        assertThat(retrievedCaseNote.getAmendments()).isEmpty();

    }

    private OffenderCaseNote transientEntity(final String offenderIdentifier) {
        return transientEntityBuilder(offenderIdentifier).build();
    }

    private OffenderCaseNoteBuilder transientEntityBuilder(final String offenderIdentifier) {
        return OffenderCaseNote.builder()
                .occurrenceDateTime(now())
                .locationId("MDI")
                .authorUsername("USER2")
                .authorUserId("some id")
                .authorName("Mickey Mouse")
                .offenderIdentifier(offenderIdentifier)
                .sensitiveCaseNoteType(genType)
                .noteText("HELLO");

    }
}
