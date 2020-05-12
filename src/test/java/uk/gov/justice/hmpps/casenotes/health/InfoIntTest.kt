package uk.gov.justice.hmpps.casenotes.health

import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE

@ActiveProfiles("noqueue")
class InfoIntTest : IntegrationTest() {
  @Test
  fun `Info page contains git information`() {
    webTestClient.get().uri("/info").exchange().expectBody().jsonPath("git.commit.id").isNotEmpty()
  }

  @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/info").exchange()
        .expectBody().jsonPath("build.version").isEqualTo(LocalDateTime.now().format(ISO_DATE))
  }
}
