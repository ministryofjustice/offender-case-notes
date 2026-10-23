package uk.gov.justice.hmpps.casenotes.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ActiveProfiles("noqueue")
class InfoIntTest : BasicIntegrationTest() {
  @Test
  fun `Info page contains git information`() {
    webTestClient.get().uri("/info").exchange().expectBody().jsonPath("git.commit.id").isNotEmpty()
  }

  @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/info").exchange()
      .expectBody().jsonPath("build.version").value<String> {
        assertThat(it).startsWith(DateTimeFormatter.ISO_DATE.format(LocalDate.now()))
      }
  }
}
