package uk.gov.justice.hmpps.casenotes.dto;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote;
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote.OffenderCaseNoteBuilder;
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType;
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

public class CaseNoteEventTest {

    @Test
    public void testOffenderCaseNote_NoteText_NoAmendments() {
        final var ocn = buildOffenderCaseNote()
                .build();
        assertThat(new CaseNoteEvent(ocn).getContent()).isEqualTo("HELLO");
    }

    @Test
    public void testOffenderCaseNote_NoteText() {
        final var ocn = buildOffenderCaseNote()
                .build();
        ocn.addAmendment("some amendment", "someuser", "Some Author");
        ReflectionTestUtils.setField(ocn.getAmendments().get(0), "createDateTime", LocalDateTime.parse("2019-03-01T22:21:20"));
        ocn.addAmendment("another amendment", "anotheruser", "Another Author");
        ReflectionTestUtils.setField(ocn.getAmendments().get(1), "createDateTime", LocalDateTime.parse("2019-04-02T22:21:20"));
        assertThat(new CaseNoteEvent(ocn).getContent()).isEqualTo("HELLO ...[someuser updated the case notes on 2019/03/01 22:21:20] some amendment ...[anotheruser updated the case notes on 2019/04/02 22:21:20] another amendment");
    }

    @Test
    public void testOffenderCaseNote_StaffName() {
        final var ocn = buildOffenderCaseNote().build();
        assertThat(new CaseNoteEvent(ocn).getStaffName()).isEqualTo("Mickey Mouse");
    }

    @Test
    public void testOffenderCaseNote_NoteType() {
        final var ocn = buildOffenderCaseNote().build();
        assertThat(new CaseNoteEvent(ocn).getNoteType()).isEqualTo("parent type");
    }

    private OffenderCaseNoteBuilder buildOffenderCaseNote() {
        return OffenderCaseNote.builder()
                .id(UUID.randomUUID())
                .occurrenceDateTime(now())
                .locationId("MDI")
                .authorUsername("USER2")
                .authorName("Mickey Mouse")
                .offenderIdentifier("A1234BD")
                .sensitiveCaseNoteType(SensitiveCaseNoteType.builder().type("type").parentType(ParentNoteType.builder().type("parent").build()).build())
                .noteText("HELLO");
    }
}
