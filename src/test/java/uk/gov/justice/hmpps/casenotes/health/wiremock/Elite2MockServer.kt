package uk.gov.justice.hmpps.casenotes.health.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteTypeDto
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote
import uk.gov.justice.hmpps.casenotes.utils.JsonHelper.objectMapper
import java.time.LocalDateTime

class Elite2Extension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val elite2Api = Elite2MockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    elite2Api.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    elite2Api.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    elite2Api.stop()
  }
}

class Elite2MockServer : WireMockServer(WIREMOCK_PORT) {
  fun subGetCaseNoteTypes() {
    val getCaseNoteTypes = "$API_PREFIX/reference-domains/caseNoteTypes"
    stubFor(
      get(WireMock.urlPathEqualTo(getCaseNoteTypes))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              objectMapper.writeValueAsString(
                listOf(
                  CaseNoteTypeDto.builder().code("KA").description("Key worker")
                    .subCodes(
                      listOf(
                        CaseNoteTypeDto.builder().code("KS").description("Key worker Session").build(),
                        CaseNoteTypeDto.builder().code("KE").description("Key worker Entry").build(),
                      ),
                    ).build(),
                  CaseNoteTypeDto.builder().code("OBS").description("Observation")
                    .subCodes(
                      listOf(
                        CaseNoteTypeDto.builder().code("GEN").description("General").build(),
                        CaseNoteTypeDto.builder().code("SPECIAL").description("Special").build(),
                      ),
                    ).build(),
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun subUserCaseNoteTypes() {
    val getCaseNoteTypes = "$API_PREFIX/users/me/caseNoteTypes"
    stubFor(
      get(WireMock.urlPathEqualTo(getCaseNoteTypes))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              objectMapper.writeValueAsString(
                listOf(
                  CaseNoteTypeDto.builder().code("KA").description("Key worker")
                    .subCodes(
                      listOf(
                        CaseNoteTypeDto.builder().code("KS").description("Key worker Session").build(),
                      ),
                    ).build(),
                  CaseNoteTypeDto.builder().code("OBS").description("Observation")
                    .subCodes(
                      listOf(
                        CaseNoteTypeDto.builder().code("GEN").description("General").build(),
                      ),
                    ).build(),
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun subGetOffender(offenderIdentifier: String) {
    val getCaseNoteTypes = "$API_PREFIX/bookings/offenderNo/$offenderIdentifier"
    stubFor(
      get(urlPathMatching(getCaseNoteTypes))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              "{\n" +
                "  \"bookingId\": 1,\n" +
                "  \"offenderNo\": \"" + offenderIdentifier + "\",\n" +
                "  \"agencyId\": \"LEI\"" +
                "}",
            )
            .withStatus(200),
        ),
    )
  }

  fun stubGetBookingIdentifiers(bookingIdentifier: Long) {
    val getBookingIdentifiers = "$API_PREFIX/bookings/$bookingIdentifier/identifiers"
    stubFor(
      get(urlPathMatching(getBookingIdentifiers))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """[
                    {
                      "type": "MERGED",
                      "value": "A2345CD",
                      "offenderNo": "A5156DY",
                      "bookingId": $bookingIdentifier,
                      "caseloadType": "INST"
                    },
                    {
                      "type": "MERGED",
                      "value": "A1234BC",
                      "offenderNo": "A1234BC",
                      "bookingId": $bookingIdentifier,
                      "caseloadType": "INST"
                    }
                  ]""",
            )
            .withStatus(200),
        ),
    )
  }

  fun stubGetBookingBasicInfo(bookingIdentifier: Long) {
    val getBookingBasicInfo = "$API_PREFIX/bookings/$bookingIdentifier?basicInfo=true"
    stubFor(
      get(urlEqualTo(getBookingBasicInfo))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """{
                    "bookingId": $bookingIdentifier,
                    "bookingNo": "38408A",
                    "offenderNo": "A5156DY",
                    "firstName": "ROGER",
                    "lastName": "QUILTER",
                    "agencyId": "OUT",
                    "activeFlag": false,
                    "dateOfBirth": "1932-05-04"
                  }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun subGetCaseNotesForOffender(offenderIdentifier: String) {
    val getCaseNotes = "$API_PREFIX/offenders/$offenderIdentifier/case-notes/v2"
    val body = """ 
      {
        "content": [
          {
            "caseNoteId": 131232,
            "type": "OBS",
            "typeDescription": "Observation",
            "subType": "GEN",
            "subTypeDescription": "General",
            "source": "INST",
            "creationDateTime": "2021-06-07T14:58:14.917306",
            "occurrenceDateTime": "2021-06-07T14:58:14.917397",
            "staffId": 1231232,
            "authorName": "Mickey Mouse",
            "text": "Some Text",
            "originalNoteText": "Some Text",
            "agencyId": "LEI",
            "amendments": []
          }
        ],
        "pageable": {
          "sort": {
            "sorted": true,
            "unsorted": false,
            "empty": false
          },
          "offset": 0,
          "pageNumber": 0,
          "pageSize": 10,
          "paged": true,
          "unpaged": false
        },
        "last": true,
        "totalElements": 1,
        "totalPages": 1,
        "size": 10,
        "number": 0,
        "sort": {
          "sorted": true,
          "unsorted": false,
          "empty": false
        },
        "first": true,
        "numberOfElements": 1,
        "empty": false
      }
    """.trimIndent()
    stubFor(
      get(urlPathMatching(getCaseNotes))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .withStatus(200),
        ),
    )
  }

  fun subGetCaseNotesForOffenderNotFound(offenderIdentifier: String) {
    val getCaseNotes = "$API_PREFIX/offenders/$offenderIdentifier/case-notes/v2"
    stubFor(
      get(urlPathMatching(getCaseNotes))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                  {
                      "status": 404,
                      "userMessage": "Resource with id [A9868AN] not found.",
                      "developerMessage": "Resource with id [A9868AN] not found."
                  }
              """.trimIndent(),
            )
            .withStatus(404),
        ),
    )
  }

  fun subGetCaseNoteForOffender(offenderIdentifier: String?, caseNoteIdentifier: Long?) {
    val getCaseNote = String.format("%s/offenders/%s/case-notes/%s", API_PREFIX, offenderIdentifier, caseNoteIdentifier)
    val body = objectMapper.writeValueAsString(createNomisCaseNote())
    stubFor(
      get(urlPathMatching(getCaseNote))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .withStatus(200),
        ),
    )
  }

  private fun createNomisCaseNote(): NomisCaseNote {
    return NomisCaseNote.builder()
      .caseNoteId(131232)
      .agencyId("LEI")
      .authorName("Mickey Mouse")
      .creationDateTime(LocalDateTime.now().minusMonths(1))
      .source("INST")
      .originalNoteText("Some Text")
      .staffId(1231232L)
      .type("OBS")
      .subType("GEN")
      .typeDescription("Observation")
      .subTypeDescription("General")
      .text("Some Text")
      .occurrenceDateTime(LocalDateTime.now().minusMonths(1))
      .build()
  }

  fun subCreateCaseNote(offenderIdentifier: String?) {
    val body = objectMapper.writeValueAsString(createNomisCaseNote())
    stubFor(
      WireMock.post(urlPathMatching(String.format("%s/offenders/%s/case-notes", API_PREFIX, offenderIdentifier)))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .withStatus(201),
        ),
    )
  }

  fun subAmendCaseNote(offenderIdentifier: String?, caseNoteIdentifier: String?) {
    val body = objectMapper.writeValueAsString(createNomisCaseNote())
    stubFor(
      WireMock.put(
        urlPathMatching(
          String.format(
            "%s/offenders/%s/case-notes/%s",
            API_PREFIX,
            offenderIdentifier,
            caseNoteIdentifier,
          ),
        ),
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .withStatus(200),
        ),
    )
  }

  companion object {
    private const val WIREMOCK_PORT = 8999
    private const val API_PREFIX = "/api"
  }
}
