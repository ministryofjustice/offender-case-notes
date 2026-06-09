package uk.gov.justice.hmpps.casenotes.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import java.util.UUID

class RestoreDeletedAmendmentIntTest : IntegrationTest() {
  @Test
  @Sql("classpath:jpa/repository/reset.sql")
  @Sql("classpath:resources/casenotes/case_notes.sql")
  @Sql("classpath:resources/casenotes/deleted_case_notes.sql")
  @Sql("classpath:resources/casenotes/restore_deleted_case_note_amendment.sql")
  fun `can read a restored case note amendment on the case note it was associated with`() {
    val note = noteRepository.findById(UUID.fromString("01924e63-4d35-7033-bc72-f0eb8a61b196")).orElseThrow()
    assertThat(note.id.toString()).isEqualTo("01924e63-4d35-7033-bc72-f0eb8a61b196")
    assertThat(note.amendments().size).isEqualTo(1)
    val amendment = note.amendments().first
    assertThat(amendment.id.toString()).isEqualTo("0192007f-0ec6-72b8-ad06-509414afaeee")
    assertThat(amendment.text).isEqualTo("test amendment 2")
  }
}
