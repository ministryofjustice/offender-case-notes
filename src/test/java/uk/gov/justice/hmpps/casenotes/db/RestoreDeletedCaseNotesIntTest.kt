package uk.gov.justice.hmpps.casenotes.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import java.util.UUID

class RestoreDeletedCaseNotesIntTest : IntegrationTest() {
  @Test
  @Sql("classpath:jpa/repository/reset.sql")
  @Sql("classpath:resources/casenotes/deleted_case_notes.sql")
  @Sql("classpath:resources/casenotes/restore_deleted_case_notes.sql")
  fun `can read a restored case note by id`() {
    val note = noteRepository.findById(UUID.fromString("0192007e-abff-7acb-9282-341ffff52e29")).orElseThrow()
    assertThat(note.id.toString()).isEqualTo("0192007e-abff-7acb-9282-341ffff52e29")
    assertThat(note.amendments().size).isEqualTo(2)
  }
}
