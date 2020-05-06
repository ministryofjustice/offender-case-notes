package uk.gov.justice.hmpps.casenotes.health

import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE

class InfoIntTest : IntegrationTest() {
  @Test
  fun `Info page contains git information`() {
    val response = restTemplate.getForEntity("/info", String::class.java)
    assertThatJson(response.body!!).node("git.commit.id").isPresent()
  }

  @Test
  fun `Info page reports version`() {
    val response = restTemplate.getForEntity("/info", String::class.java)
    assertThatJson(response.body!!).node("build.version").isEqualTo(LocalDateTime.now().format(ISO_DATE))
  }
}
