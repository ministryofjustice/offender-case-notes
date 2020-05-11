package uk.gov.justice.hmpps.casenotes.health.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteType
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Elite2MockServer : WireMockRule(WIREMOCK_PORT) {
  private val gson: Gson
  fun subGetCaseNoteTypes() {
    val getCaseNoteTypes = "$API_PREFIX/reference-domains/caseNoteTypes"
    stubFor(
        WireMock.get(WireMock.urlPathEqualTo(getCaseNoteTypes))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(gson.toJson(listOf(
                    CaseNoteType.builder().code("KA").description("Key worker")
                        .subCodes(listOf(
                            CaseNoteType.builder().code("KS").description("Key worker Session").build(),
                            CaseNoteType.builder().code("KE").description("Key worker Entry").build()
                        )),
                    CaseNoteType.builder().code("OBS").description("Observation")
                        .subCodes(listOf(
                            CaseNoteType.builder().code("GEN").description("General").build(),
                            CaseNoteType.builder().code("SPECIAL").description("Special").build()
                        )
                        ))))
                .withStatus(200)
            ))
  }

  fun subUserCaseNoteTypes() {
    val getCaseNoteTypes = "$API_PREFIX/users/me/caseNoteTypes"
    stubFor(
        WireMock.get(WireMock.urlPathEqualTo(getCaseNoteTypes))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(gson.toJson(listOf(
                    CaseNoteType.builder().code("KA").description("Key worker")
                        .subCodes(listOf(
                            CaseNoteType.builder().code("KS").description("Key worker Session").build()
                        )),
                    CaseNoteType.builder().code("OBS").description("Observation")
                        .subCodes(listOf(
                            CaseNoteType.builder().code("GEN").description("General").build()
                        )
                        ))))
                .withStatus(200)
            ))
  }

  fun subGetOffender(offenderIdentifier: String) {
    val getCaseNoteTypes = "$API_PREFIX/bookings/offenderNo/$offenderIdentifier"
    stubFor(
        WireMock.get(WireMock.urlPathMatching(getCaseNoteTypes))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\n" +
                    "  \"bookingId\": 1,\n" +
                    "  \"offenderNo\": \"" + offenderIdentifier + "\",\n" +
                    "  \"agencyId\": \"LEI\"" +
                    "}")
                .withStatus(200)
            ))
  }

  fun subGetCaseNotesForOffender(offenderIdentifier: String) {
    val getCaseNotes = "$API_PREFIX/offenders/$offenderIdentifier/case-notes"
    val body = gson.toJson(listOf(createNomisCaseNote()))
    stubFor(
        WireMock.get(WireMock.urlPathMatching(getCaseNotes))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withHeader("Total-Records", "1")
                .withHeader("Page-Offset", "0")
                .withHeader("Page-Limit", "10")
                .withBody(body)
                .withStatus(200)
            ))
  }

  fun subGetCaseNotesForOffenderNotFound(offenderIdentifier: String) {
    val getCaseNotes = "$API_PREFIX/offenders/$offenderIdentifier/case-notes"
    stubFor(
        WireMock.get(WireMock.urlPathMatching(getCaseNotes))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withHeader("Total-Records", "1")
                .withHeader("Page-Offset", "0")
                .withHeader("Page-Limit", "10")
                .withBody("""
                  {
                      "status": 404,
                      "userMessage": "Resource with id [A9868AN] not found.",
                      "developerMessage": "Resource with id [A9868AN] not found."
                  }
                """.trimIndent())
                .withStatus(404)
            ))
  }

  fun subGetCaseNoteForOffender(offenderIdentifier: String?, caseNoteIdentifier: Long?) {
    val getCaseNote = String.format("%s/offenders/%s/case-notes/%s", API_PREFIX, offenderIdentifier, caseNoteIdentifier)
    val body = gson.toJson(createNomisCaseNote())
    stubFor(
        WireMock.get(WireMock.urlPathMatching(getCaseNote))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)
                .withStatus(200)
            ))
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
    val body = gson.toJson(createNomisCaseNote())
    stubFor(WireMock.post(WireMock.urlPathMatching(String.format("%s/offenders/%s/case-notes", API_PREFIX, offenderIdentifier)))
        .willReturn(WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .withStatus(201)
        ))
  }

  fun subAmendCaseNote(offenderIdentifier: String?, caseNoteIdentifier: String?) {
    val body = gson.toJson(createNomisCaseNote())
    stubFor(WireMock.put(WireMock.urlPathMatching(String.format("%s/offenders/%s/case-notes/%s", API_PREFIX, offenderIdentifier, caseNoteIdentifier)))
        .willReturn(WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .withStatus(200)
        ))
  }

  private class LocalDateTimeConverter : JsonSerializer<LocalDateTime?>, JsonDeserializer<LocalDateTime> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDateTime {
      return LocalDateTime.parse(json.asJsonPrimitive.asString)
    }

    override fun serialize(src: LocalDateTime?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
      return JsonPrimitive(FORMATTER.format(src))
    }

    companion object {
      private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }
  }

  companion object {
    private const val WIREMOCK_PORT = 8999
    private const val API_PREFIX = "/api"
  }

  init {
    gson = GsonBuilder().registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeConverter()).create()
  }
}
