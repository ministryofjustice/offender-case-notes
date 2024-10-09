package uk.gov.justice.hmpps.casenotes.controllers

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.hmpps.casenotes.legacy.dto.SubjectAccessRequestData
import uk.gov.justice.hmpps.casenotes.legacy.service.SubjectAccessRequestService
import java.time.LocalDate
import java.time.LocalDateTime

class SubjectAccessRequestControllerTest : IntegrationTest() {

  @MockBean
  private lateinit var service: SubjectAccessRequestService

  @Test
  fun `should return 200 response when prisoner found and no dates provided`() {
    whenever(service.getCaseNotes("123456", null, null)).thenReturn(
      listOf(
        createOffenderCaseNote(),
      ),
    )

    webTestClient.get().uri("subject-access-request?prn=123456")
      .headers(addBearerAuthorisation("API_TEST_USER", listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(
        """{"content":
                  [{"type":"SA","subType":"SARTH","creationDateTime":"2019-02-03T23:20:19","authorName":"Tom Smith","text":null,"amendments":null}]
                }
        """.trimIndent(),
      )

    verify(service).getCaseNotes("123456", null, null)
  }

  @Test
  fun `should return 200 response when prisoner found and from date provided`() {
    val fromDate = LocalDate.now()

    whenever(service.getCaseNotes("123456", fromDate, null)).thenReturn(
      listOf(
        createOffenderCaseNote(),
      ),
    )

    webTestClient.get().uri("subject-access-request?prn=123456&fromDate=$fromDate")
      .headers(addBearerAuthorisation("API_TEST_USER", listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk

    verify(service).getCaseNotes("123456", fromDate, null)
  }

  @Test
  fun `should return 200 response when prisoner found and to date provided`() {
    val toDate = LocalDate.now()

    whenever(service.getCaseNotes("123456", null, toDate)).thenReturn(
      listOf(
        createOffenderCaseNote(),
      ),
    )

    webTestClient.get().uri("subject-access-request?prn=123456&toDate=$toDate")
      .headers(addBearerAuthorisation("API_TEST_USER", listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk

    verify(service).getCaseNotes("123456", null, toDate)
  }

  @Test
  fun `should return 204 response when prisoner not found`() {
    val toDate = LocalDate.now()

    whenever(service.getCaseNotes("123456", null, toDate)).thenReturn(
      emptyList(),
    )

    webTestClient.get().uri("subject-access-request?prn=123456&toDate=$toDate")
      .headers(addBearerAuthorisation("API_TEST_USER", listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isNoContent

    verify(service).getCaseNotes("123456", null, toDate)
  }

  @Test
  fun `should return 400 response when prisoner number empty`() {
    val toDate = LocalDate.now()

    webTestClient.get().uri("subject-access-request?prn= &toDate=$toDate")
      .headers(addBearerAuthorisation("API_TEST_USER", listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isBadRequest

    verifyNoInteractions(service)
  }

  @Test
  fun `should return 209 response for unsupported search type`() {
    webTestClient.get().uri("subject-access-request?crn=123456")
      .headers(addBearerAuthorisation("API_TEST_USER", listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .json(
        "{" +
          "'status':209," +
          "'userMessage':'Search by case reference number is not supported.'," +
          "'developerMessage':'Search by case reference number is not supported.'" +
          "}",
      )

    verifyNoInteractions(service)
  }

  @Test
  fun `should return 403 response for forbidden`() {
    webTestClient.get().uri("subject-access-request?prn=123456")
      .headers(addBearerAuthorisation("API_TEST_USER"))
      .exchange()
      .expectStatus().isForbidden

    verifyNoInteractions(service)
  }

  @Test
  fun `should return 403 response for missing SAR_DATA_ACCESS role`() {
    webTestClient.get().uri("subject-access-request?prn=123456")
      .exchange()
      .expectStatus().isUnauthorized

    verifyNoInteractions(service)
  }

  private fun createOffenderCaseNote(): SubjectAccessRequestData =
    SubjectAccessRequestData.builder()
      .authorName("Tom Smith")
      .type("SA")
      .creationDateTime(LocalDateTime.parse("2019-02-03T23:20:19"))
      .subType("SARTH")
      .build()
}
