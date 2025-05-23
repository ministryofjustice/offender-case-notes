@file:Suppress("ClassName")

package uk.gov.justice.hmpps.casenotes.controllers

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.havingExactly
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.matching.ExactMatchMultiValuePattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_ADMIN
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension.Companion.elite2Api
import uk.gov.justice.hmpps.casenotes.health.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.hmpps.casenotes.health.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.hmpps.casenotes.legacy.repository.CaseNoteSubTypeRepository
import uk.gov.justice.hmpps.casenotes.legacy.repository.OffenderCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.legacy.service.ExternalApiService
import uk.gov.justice.hmpps.casenotes.notes.CaseNote

class CaseNoteResourceTest : IntegrationTest() {

  @MockitoSpyBean
  internal lateinit var externalApiService: ExternalApiService

  @Autowired
  internal lateinit var caseNoteSubTypeRepository: CaseNoteSubTypeRepository

  @Autowired
  internal lateinit var ocnRepository: OffenderCaseNoteRepository

  @Nested
  inner class getCaseNotes {
    @Test
    fun testRetrieveCaseNotesForOffenderSensitive() {
      manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
      prisonerSearchApi.stubPrisonerDetails("A1234AA", "LEI")
      elite2Api.subGetCaseNotesForOffender("A1234AA")
      val token = jwtHelper.createJwt("SECURE_CASENOTE_USER", roles = CASENOTES_ROLES)
      webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AA")
        .headers(addBearerToken(token))
        .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is a case note"))
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .json(readFile("A1234AA-create-casenote.json"))
      webTestClient.get().uri("/case-notes/{offenderIdentifier}", "A1234AA")
        .headers(addBearerToken(token))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(readFile("A1234AA-casenote.json"))
    }

    @Test
    fun testCanRetrieveCaseNotesForOffenderNormal() {
      manageUsersApi.stubGetUserDetails("API_TEST_USER")
      elite2Api.subGetCaseNotesForOffender("A1234AA")
      webTestClient.get().uri("/case-notes/{offenderIdentifier}", "A1234AA")
        .headers(addBearerAuthorisation("API_TEST_USER"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(readFile("A1234AA-normal-casenote.json"))
    }

    @Test
    fun `test can retrieve case notes - check Prison API default query`() {
      manageUsersApi.stubGetUserDetails("API_TEST_USER")
      elite2Api.subGetCaseNotesForOffender("A1234AA")
      webTestClient.get().uri("/case-notes/{offenderIdentifier}", "A1234AA")
        .headers(addBearerAuthorisation("API_TEST_USER"))
        .exchange()
        .expectStatus().isOk

      elite2Api.verify(
        getRequestedFor(urlPathEqualTo("/api/offenders/A1234AA/case-notes/v2"))
          .withQueryParam("size", equalTo("10"))
          .withQueryParam("page", equalTo("0"))
          .withQueryParam("sort", equalTo("occurrenceDateTime,DESC")),
      )
    }

    @Test
    fun `test can retrieve case notes - check Prison API parameters`() {
      manageUsersApi.stubGetUserDetails("API_TEST_USER")
      elite2Api.subGetCaseNotesForOffender("A1234AA")
      webTestClient.get().uri {
        it.path("/case-notes/{offenderIdentifier}")
          .queryParam("type", "GEN")
          .queryParam("subType", "OSI")
          .queryParam("locationId", "MDI")
          .queryParam("startDate", "2024-01-02T10:20:30")
          .queryParam("endDate", "2024-02-01T12:10:05")
          .queryParam("size", "20")
          .queryParam("page", "5")
          .queryParam("sort", "creationDateTime,ASC")
          .build("A1234AA")
      }
        .headers(addBearerAuthorisation("API_TEST_USER"))
        .exchange()
        .expectStatus().isOk

      elite2Api.verify(
        getRequestedFor(urlPathEqualTo("/api/offenders/A1234AA/case-notes/v2"))
          .withQueryParam("typeSubTypes", equalTo("GEN+OSI"))
          .withQueryParam("prisonId", equalTo("MDI"))
          .withQueryParam("from", equalTo("2024-01-02"))
          .withQueryParam("to", equalTo("2024-02-01"))
          .withQueryParam("size", equalTo("20"))
          .withQueryParam("page", equalTo("5"))
          .withQueryParam("sort", equalTo("createDatetime,ASC")),
      )
    }

    @Test
    fun `test subType must be used in conjunction with type`() {
      manageUsersApi.stubGetUserDetails("API_TEST_USER")
      elite2Api.subGetCaseNotesForOffender("A1234AA")
      webTestClient.get().uri {
        it.path("/case-notes/{offenderIdentifier}")
          .queryParam("subType", "OSI")
          .queryParam("locationId", "MDI")
          .queryParam("startDate", "2024-01-02T10:20:30")
          .queryParam("endDate", "2024-02-01T12:10:05")
          .queryParam("size", "20")
          .queryParam("page", "5")
          .queryParam("sort", "creationDateTime,ASC")
          .build("A1234AA")
      }
        .headers(addBearerAuthorisation("API_TEST_USER"))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .json(
          "{" +
            "'status':400," +
            "'developerMessage':'SubType must be used in conjunction with type.'" +
            "}",
        )
    }

    @Test
    fun `test can retrieve case notes - check Prison API parameters for additional types`() {
      manageUsersApi.stubGetUserDetails("API_TEST_USER")
      elite2Api.subGetCaseNotesForOffender("A1234AA")
      webTestClient.get().uri {
        it.path("/case-notes/{offenderIdentifier}")
          .queryParam("locationId", "MDI")
          .queryParam("startDate", "2024-01-02T10:20:30")
          .queryParam("endDate", "2024-02-01T12:10:05")
          .queryParam("type", "GEN")
          .queryParam("subType", "OSI")
          .queryParam("size", "20")
          .queryParam("page", "5")
          .queryParam("sort", "creationDateTime,ASC")
          .build("A1234AA")
      }
        .headers(addBearerAuthorisation("API_TEST_USER"))
        .exchange()
        .expectStatus().isOk

      elite2Api.verify(
        getRequestedFor(urlPathEqualTo("/api/offenders/A1234AA/case-notes/v2"))
          .withQueryParam("from", equalTo("2024-01-02"))
          .withQueryParam("to", equalTo("2024-02-01"))
          .withQueryParam("typeSubTypes", havingExactly("GEN+OSI"))
          .withQueryParam("size", equalTo("20"))
          .withQueryParam("page", equalTo("5"))
          .withQueryParam("sort", equalTo("createDatetime,ASC")),
      )
    }

    @Test
    fun `test can retrieve case notes - query params passed through`() {
      manageUsersApi.stubGetUserDetails("API_TEST_USER")
      elite2Api.subGetCaseNotesForOffender("A1234AA")
      webTestClient.get().uri {
        it.path("/case-notes/{offenderIdentifier}")
          .queryParam("size", "30")
          .queryParam("page", "5")
          .queryParam("sort", "creationDateTime,ASC")
          .queryParam("sort", "occurrenceDateTime,DESC")
          .build("A1234AA")
      }
        .headers(addBearerAuthorisation("API_TEST_USER"))
        .exchange()
        .expectStatus().isOk

      elite2Api.verify(
        getRequestedFor(urlPathEqualTo("/api/offenders/A1234AA/case-notes/v2"))
          .withQueryParam("size", equalTo("30"))
          .withQueryParam("page", equalTo("5"))
          .withQueryParam(
            "sort",
            ExactMatchMultiValuePattern(
              listOf(
                equalTo("createDatetime,ASC"),
                equalTo("occurrenceDateTime,DESC"),
              ),
            ),
          ),
      )
    }

    @Test
    fun testRetrieveCaseNotesWillReturn404IfOffenderNotFound() {
      manageUsersApi.stubGetUserDetails("API_TEST_USER")
      elite2Api.subGetCaseNotesForOffenderNotFound("A1234AA")
      webTestClient.get().uri("/case-notes/{offenderIdentifier}", "A1234AA")
        .headers(addBearerAuthorisation("API_TEST_USER"))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .json(readFile("offender-not-found.json"))
    }

    @Test
    fun testCanRetrieveCaseNoteForOffender() {
      manageUsersApi.stubGetUserDetails("API_TEST_USER")
      elite2Api.subGetCaseNoteForOffender("A1234AA", 131232L)
      webTestClient.get().uri("/case-notes/{offenderIdentifier}/{caseNoteIdentifier}", "A1234AA", "131232")
        .headers(addBearerAuthorisation("API_TEST_USER"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(readFile("A1234AA-single-normal-casenote.json"))
    }

    @Test
    fun testRetrieveCaseNoteForOffenderSensitive() {
      manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
      prisonerSearchApi.stubPrisonerDetails("A1234AF", "LEI")
      val token = jwtHelper.createJwt("SECURE_CASENOTE_USER", roles = CASENOTES_ROLES)
      val postResponse = webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AF")
        .headers(addBearerToken(token))
        .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is a case note"))
        .exchange()
        .expectStatus().isCreated
        .returnResult(CaseNote::class.java)
      val id = postResponse.responseBody.blockFirst()!!.id
      webTestClient.get().uri("/case-notes/{offenderIdentifier}/{caseNoteIdentifier}", "A1234AF", id)
        .headers(addBearerToken(token))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(readFile("A1234AF-single-casenote.json"))
    }

    @Test
    fun `case notes of type sync to nomis stored in the db are not returned`() {
      manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
      val personIdentifier = "S1234TN"
      elite2Api.subGetCaseNotesForOffender(personIdentifier)
      val token = jwtHelper.createJwt("SECURE_CASENOTE_USER", roles = CASENOTES_ROLES)

      webTestClient.get().uri("/case-notes/{personIdentifier}", personIdentifier)
        .headers(addBearerToken(token))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          """
          {
            "totalElements": 1,
            "totalPages": 1,
            "sort": {
              "empty": false,
              "unsorted": false,
              "sorted": true
            },
            "first": true,
            "last": true,
            "number": 0,
            "size": 10,
            "content": [
              {
                "caseNoteId": "131232",
                "offenderIdentifier": "S1234TN",
                "type": "OBS",
                "typeDescription": "Observation",
                "subType": "GEN",
                "subTypeDescription": "General",
                "source": "INST",
                "creationDateTime": "2021-06-07T14:58:14.917306",
                "occurrenceDateTime": "2021-06-07T14:58:14.917397",
                "authorName": "John Smith",
                "authorUserId": "1231232",
                "text": "Some Text",
                "locationId": "LEI",
                "eventId": 131232,
                "sensitive": false,
                "amendments": [],
                "systemGenerated": false,
                "legacyId": 131232
              }
            ],
            "numberOfElements": 1,
            "pageable": {
              "pageNumber": 0,
              "pageSize": 10,
              "sort": {
                "empty": false,
                "unsorted": false,
                "sorted": true
              },
              "offset": 0,
              "unpaged": false,
              "paged": true
            },
            "empty": false
          }
          """.trimIndent(),
        )
    }
  }

  @Test
  fun `request with username longer than 64 characters fails with validation exception`() {
    val username = "A_VERY_LONG_USERNAME_THAT_EXCEEDS_THE_SIXTY_FOUR_CHARACTERS_LIMIT"
    manageUsersApi.stubGetUserDetails(username)
    elite2Api.subCreateCaseNote("A1234AE")

    // create the case note
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AE")
      .headers(addBearerAuthorisation(username, CASENOTES_ROLES))
      .bodyValue(CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC.format("This is another case note"))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .json("""{"developerMessage": "username for audit exceeds 64 characters"}""")
  }

  @Test
  fun `request with username of 64 characters succeeds`() {
    val username = "A_VERY_LONG_USERNAME_THAT_NEARLY_THE_SIXTY_FOUR_CHARACTERS_LIMIT"
    manageUsersApi.stubGetUserDetails(username)
    prisonerSearchApi.stubPrisonerDetails("A1234AD", "LEI")

    // create the case note
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AD")
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
      .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is another case note"))
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .json(readFile("A1234AD-create-casenote.json"))
  }

  @Test
  fun testCanCreateCaseNote_Normal() {
    manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
    elite2Api.subCreateCaseNote("A1234AE")
    prisonerSearchApi.stubPrisonerDetails("A1234AE", "LEI")

    // create the case note
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AE")
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
      .bodyValue(CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC.format("This is another case note"))
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .json(readFile("A1234AE-create-casenote.json"))
  }

  @Test
  fun testCanCreateCaseNote_Secure() {
    manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
    prisonerSearchApi.stubPrisonerDetails("A1234AD", "LEI")

    // create the case note
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AD")
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
      .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is another case note"))
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .json(readFile("A1234AD-create-casenote.json"))
  }

  @Test
  fun `create a case note with type sync to nomis is sent to prison api`() {
    manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
    prisonerSearchApi.stubPrisonerDetails("N1234CT")
    elite2Api.subCreateCaseNote("N1234CT")

    // create the case note
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "N1234CT")
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
      .bodyValue(
        CREATE_CASE_NOTE_BY_TYPE.format(
          "ACP",
          "ASSESSMENT",
          "This should not be created in dps",
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody().jsonPath("source").isEqualTo("INST")

    verify(externalApiService).createCaseNote(eq("N1234CT"), any())
  }

  @Test
  fun testCanCreateCaseNote_SecureWithPomRole() {
    manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
    prisonerSearchApi.stubPrisonerDetails("A1234AD", "LEI")

    // create the case note
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AD")
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", POM_ROLE))
      .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is another case note"))
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .json(readFile("A1234AD-create-casenote.json"))
  }

  @Test
  fun testCannotCreateInactiveCaseNote_Secure() {
    manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
    prisonerSearchApi.stubPrisonerDetails("A1234AD")

    // create the case note
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AD")
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
      .bodyValue(
        CREATE_CASE_NOTE_BY_TYPE.format(
          "OLDPOM",
          "OLDTWO",
          "This is another case note with inactive case note type",
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun testCanCreateAmendments() {
    manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
    prisonerSearchApi.stubPrisonerDetails("A1234AB", "LEI")
    elite2Api.subGetCaseNotesForOffender("A1234AB")
    val token = jwtHelper.createJwt("SECURE_CASENOTE_USER", roles = CASENOTES_ROLES)

    // create the case note
    val postResponse = webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AB")
      .headers(addBearerToken(token))
      .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is another case note"))
      .exchange()
      .expectStatus().isCreated
      .returnResult(CaseNote::class.java)

    // amend the case note
    webTestClient.put().uri(
      "/case-notes/{offenderIdentifier}/{caseNoteId}",
      "A1234AB",
      postResponse.responseBody.blockFirst()!!.id,
    )
      .headers(addBearerToken(token))
      .bodyValue("""{ "text": "Amended case note" }""")
      .exchange()
      .expectStatus().isOk
      .expectBody().json(readFile("A1234AB-update-casenote.json"))

    // check the case note now correct
    webTestClient.get().uri("/case-notes/{offenderIdentifier}", "A1234AB")
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("A1234AB-casenote.json"))
  }

  @Test
  fun testCanCreateAmendments_Normal() {
    manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
    elite2Api.subAmendCaseNote("A1234AE", "12345")

    // amend the case note
    webTestClient.put().uri("/case-notes/{offenderIdentifier}/{caseNoteId}", "A1234AE", "12345")
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
      .bodyValue("""{ "text": "Amended case note" }""")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("A1234AE-create-casenote.json"))
  }

  @Test
  fun testCanFilterCaseNotes() {
    manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
    prisonerSearchApi.stubPrisonerDetails("A1234AC", "LEI")
    elite2Api.subGetCaseNotesForOffender("A1234AC")
    val token = jwtHelper.createJwt("SECURE_CASENOTE_USER", roles = CASENOTES_ROLES)
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AC")
      .headers(addBearerToken(token))
      .bodyValue(CREATE_CASE_NOTE.format("MDI", "This is a case note 1"))
      .exchange()
      .expectStatus().isCreated
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AC")
      .headers(addBearerToken(token))
      .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is a case note 2"))
      .exchange()
      .expectStatus().isCreated
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AC")
      .headers(addBearerToken(token))
      .bodyValue(CREATE_CASE_NOTE.format("LEI", "This is a case note 3"))
      .exchange()
      .expectStatus().isCreated
    webTestClient.get().uri("/case-notes/{offenderIdentifier}?size={size}&page={page}", "A1234AC", "2", "1")
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("A1234AC-casenote.json"))
  }

  @Test
  fun testSoftDeleteCaseNoteUserDoesntHaveRole() {
    manageUsersApi.stubGetUserDetails("SECURE_CASENOTE_USER")
    val token = jwtHelper.createJwt("SECURE_CASENOTE_USER", roles = CASENOTES_ROLES)

    webTestClient.delete()
      .uri("/case-notes/{offenderIdentifier}/{caseNoteId}", "Z1234ZZ", "231eb4ee-c06c-49a3-846c-1b542cc0ed6b")
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun testDeleteCaseNoteNotFound() {
    manageUsersApi.stubGetUserDetails("DELETE_CASE_NOTE_USER")
    val token = jwtHelper.createJwt("DELETE_CASE_NOTE_USER", roles = listOf(ROLE_CASE_NOTES_ADMIN))
    webTestClient.delete()
      .uri("/case-notes/{offenderIdentifier}/{caseNoteId}", "Z1234ZZ", "231eb4ee-c06c-49a3-846c-1b542cc0ed6b")
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .json(
        "{" +
          "'status':404," +
          "'developerMessage':'Resource with id [231eb4ee-c06c-49a3-846c-1b542cc0ed6b] not found.'" +
          "}",
      )
  }

  companion object {
    private const val CREATE_CASE_NOTE =
      """{"locationId": "%s", "type": "POM", "subType": "GEN", "text": "%s"}"""
    private const val CREATE_CASE_NOTE_WITHOUT_LOC =
      """{"type": "POM", "subType": "GEN", "text": "%s"}"""
    private const val CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC =
      """{"type": "ACP", "subType": "POS1", "text": "%s"}"""
    private const val CREATE_CASE_NOTE_BY_TYPE =
      """{"type": "%s", "subType": "%s", "text": "%s"}"""
    private val POM_ROLE = listOf("ROLE_POM")
    private val CASENOTES_ROLES = listOf("ROLE_VIEW_SENSITIVE_CASE_NOTES", "ROLE_ADD_SENSITIVE_CASE_NOTES")
  }
}
