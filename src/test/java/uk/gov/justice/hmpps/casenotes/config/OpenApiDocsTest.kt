package uk.gov.justice.hmpps.casenotes.config

import com.fasterxml.jackson.core.JsonProcessingException
import io.swagger.v3.parser.OpenAPIV3Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import springfox.documentation.spring.web.json.Json
import springfox.documentation.spring.web.json.JsonSerializer
import uk.gov.justice.hmpps.casenotes.controllers.ResourceTest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class OpenApiDocsTest : ResourceTest() {

  @LocalServerPort
  private var port: Int = 0

  @Test
  fun `open api docs are available`() {
    webTestClient.get()
      .uri("/swagger-ui/?configUrl=/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `the open api json reports no errors`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("messages").doesNotExist()
  }

  @Test
  fun `the open api json contains the version number`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("info.version").isEqualTo(DateTimeFormatter.ISO_DATE.format(LocalDate.now()))
  }

  @Test
  fun `the open api json is valid and contains documentation`() {
    val result = OpenAPIV3Parser().readLocation("http://localhost:$port/v3/api-docs", null, null)
    assertThat(result.openAPI.paths).isNotEmpty
    assertThat(result.messages).hasSizeLessThan(3)
  }

  @TestConfiguration
  class SwaggerSerializationConfig {

    @Bean
    @Primary
    fun swaggerJsonSerializer(): JsonSerializer = SwaggerJsonSerializer()

    class SwaggerJsonSerializer : JsonSerializer(listOf()) {
      private val objectMapper = io.swagger.v3.core.util.Json.mapper()

      override fun toJson(toSerialize: Any?): Json = try {
        Json(objectMapper.writeValueAsString(toSerialize))
      } catch (e: JsonProcessingException) {
        throw RuntimeException("Could not write JSON", e)
      }
    }
  }
}
