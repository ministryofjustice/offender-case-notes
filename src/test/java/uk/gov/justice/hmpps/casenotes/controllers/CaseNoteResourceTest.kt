@file:Suppress("ClassName")

package uk.gov.justice.hmpps.casenotes.controllers

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.havingExactly
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.matching.ExactMatchMultiValuePattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension.Companion.elite2Api
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi

class CaseNoteResourceTest : ResourceTest() {
  @Test
  fun testGetCaseNoteTypesNormal() {
    elite2Api.subGetCaseNoteTypes()
    webTestClient.get().uri("/case-notes/types")
      .headers(addBearerAuthorisation("API_TEST_USER"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("caseNoteTypes.json"))
  }

  @Test
  fun testGetCaseNoteClientTokenNoUserId() {
    elite2Api.subGetCaseNoteTypes()
    webTestClient.get().uri("/case-notes/types")
      .headers(addBearerToken(jwtHelper.createJwt("API_TEST_USER", userId = null)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("caseNoteTypes.json"))
  }

  @Test
  fun testGetCaseNoteTypesSecure() {
    elite2Api.subGetCaseNoteTypes()
    webTestClient.get().uri("/case-notes/types")
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("caseNoteTypesSecure.json"))
  }

  @Test
  fun testGetCaseNoteTypesSecurePomRole() {
    elite2Api.subGetCaseNoteTypes()
    webTestClient.get().uri("/case-notes/types")
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", POM_ROLE))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("caseNoteTypesSecure.json"))
  }

  @Test
  fun testUserCaseNoteTypesNormal() {
    elite2Api.subUserCaseNoteTypes()
    webTestClient.get().uri("/case-notes/types-for-user")
      .headers(addBearerAuthorisation("API_TEST_USER"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("userCaseNoteTypes.json"))
  }

  @Test
  fun testUserCaseNoteTypesSecure() {
    elite2Api.subUserCaseNoteTypes()
    webTestClient.get().uri("/case-notes/types-for-user")
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("userCaseNoteTypesSecure.json"))
  }

  @Test
  fun testUserCaseNoteTypesSecurePomRole() {
    elite2Api.subUserCaseNoteTypes()
    webTestClient.get().uri("/case-notes/types-for-user")
      .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", POM_ROLE))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("userCaseNoteTypesSecure.json"))
  }

  @Nested
  inner class getCaseNotes {
    @Test
    fun testRetrieveCaseNotesForOffenderSensitive() {
      oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
      elite2Api.subGetOffender("A1234AA")
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
      oAuthApi.subGetUserDetails("API_TEST_USER")
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
      oAuthApi.subGetUserDetails("API_TEST_USER")
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
      oAuthApi.subGetUserDetails("API_TEST_USER")
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
    fun `test subType must used in conjunction with type`() {
      oAuthApi.subGetUserDetails("API_TEST_USER")
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
            "'developerMessage':'SubType must used in conjunction with type.'" +
            "}",
        )
    }

    @Test
    fun `test cannot set both type and typSubTypes`() {
      oAuthApi.subGetUserDetails("API_TEST_USER")
      elite2Api.subGetCaseNotesForOffender("A1234AA")
      webTestClient.get().uri {
        it.path("/case-notes/{offenderIdentifier}")
          .queryParam("type", "GEN")
          .queryParam("subType", "OSI")
          .queryParam("locationId", "MDI")
          .queryParam("startDate", "2024-01-02T10:20:30")
          .queryParam("endDate", "2024-02-01T12:10:05")
          .queryParam("typeSubTypes", "GEN+OSI")
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
            "'developerMessage':'Both type and typeSubTypes are set, please only use one to filter.'" +
            "}",
        )
    }

    @Test
    fun `test can retrieve case notes - check Prison API parameters for additional types`() {
      oAuthApi.subGetUserDetails("API_TEST_USER")
      elite2Api.subGetCaseNotesForOffender("A1234AA")
      webTestClient.get().uri {
        it.path("/case-notes/{offenderIdentifier}")
          .queryParam("locationId", "MDI")
          .queryParam("startDate", "2024-01-02T10:20:30")
          .queryParam("endDate", "2024-02-01T12:10:05")
          .queryParam("typeSubTypes", "{typeSubTypes1}")
          .queryParam("typeSubTypes", "{typeSubTypes2}")
          .queryParam("size", "20")
          .queryParam("page", "5")
          .queryParam("sort", "creationDateTime,ASC")
          .build("A1234AA", "GEN+OSI", "APP")
      }
        .headers(addBearerAuthorisation("API_TEST_USER"))
        .exchange()
        .expectStatus().isOk

      elite2Api.verify(
        getRequestedFor(urlPathEqualTo("/api/offenders/A1234AA/case-notes/v2"))
          .withQueryParam("from", equalTo("2024-01-02"))
          .withQueryParam("to", equalTo("2024-02-01"))
          .withQueryParam("typeSubTypes", havingExactly("GEN+OSI", "APP"))
          .withQueryParam("size", equalTo("20"))
          .withQueryParam("page", equalTo("5"))
          .withQueryParam("sort", equalTo("createDatetime,ASC")),
      )
    }

    @Test
    fun `test can retrieve case notes - query params passed through`() {
      oAuthApi.subGetUserDetails("API_TEST_USER")
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
      oAuthApi.subGetUserDetails("API_TEST_USER")
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
      oAuthApi.subGetUserDetails("API_TEST_USER")
      elite2Api.subGetOffender("A1234AA")
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
      oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
      elite2Api.subGetOffender("A1234AF")
      val token = jwtHelper.createJwt("SECURE_CASENOTE_USER", roles = CASENOTES_ROLES)
      val postResponse = webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AF")
        .headers(addBearerToken(token))
        .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is a case note"))
        .exchange()
        .expectStatus().isCreated
        .returnResult(CaseNote::class.java)
      val id = postResponse.responseBody.blockFirst()!!.caseNoteId
      webTestClient.get().uri("/case-notes/{offenderIdentifier}/{caseNoteIdentifier}", "A1234AF", id)
        .headers(addBearerToken(token))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(readFile("A1234AF-single-casenote.json"))
    }
  }

  @Test
  fun testCanCreateCaseNote_Normal() {
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
    elite2Api.subCreateCaseNote("A1234AE")

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
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
    elite2Api.subGetOffender("A1234AD")

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
  fun testCanCreateCaseNote_SecureWithPomRole() {
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
    elite2Api.subGetOffender("A1234AD")

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
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
    elite2Api.subGetOffender("A1234AD")

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
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
    elite2Api.subGetOffender("A1234AB")
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
      postResponse.responseBody.blockFirst()!!.caseNoteId,
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
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
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
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
    elite2Api.subGetOffender("A1234AC")
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
  fun testCanCreateAndUpdateTypes() {
    oAuthApi.subGetUserDetails("SYSTEM_USER_READ_WRITE")
    val token = jwtHelper.createJwt("SYSTEM_USER_READ_WRITE", roles = SYSTEM_ROLES)

    // add a new case note parent type called NEWTYPE1
    webTestClient.post().uri("/case-notes/types")
      .headers(addBearerToken(token))
      .bodyValue(
        """{
            "type": "NEWTYPE1",
            "description": "A New Type 1",
            "active": false
            }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .json(readFile("newCaseNoteType1.json"))

    // amend the case note type from inactive to active and change description
    webTestClient.put().uri("/case-notes/types/NEWTYPE1")
      .headers(addBearerToken(token))
      .bodyValue(
        """{ 
            "description": "Change The Desc",
            "active": true
            }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("updateCaseNoteType1.json"))

    // add a new sub case note type called NEWSUBTYPE1
    webTestClient.post().uri("/case-notes/types/NEWTYPE1")
      .headers(addBearerToken(token))
      .bodyValue(
        """{
            "type": "NEWSUBTYPE1",
            "description": "A New Sub Type 1",
            "active": false
            }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .json(readFile("newCaseNoteSubType1.json"))

    // amend the case note sub type to active and new description
    webTestClient.put().uri("/case-notes/types/NEWTYPE1/NEWSUBTYPE1")
      .headers(addBearerToken(token))
      .bodyValue(
        """{
            "description": "Change The Desc",
            "active": true
            }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("updateCaseNoteSubType1.json"))
  }

  @Test
  fun testCannotCreateAndUpdateTypesWhenInvalid() {
    oAuthApi.subGetUserDetails("SYSTEM_USER_READ_WRITE")
    val token = jwtHelper.createJwt("SYSTEM_USER_READ_WRITE", roles = SYSTEM_ROLES)

    // add a new case note parent type called TOOLONG1234567890 that is more than 12 chars
    webTestClient.post().uri("/case-notes/types")
      .headers(addBearerToken(token))
      .bodyValue(
        """{
            "type": "TOOLONG1234567890",
            "description": "Wrong!",
            "active": false
            }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().json(readFile("validation-error1.json"))

    // amend the case note type and use an invalid boolean value
    webTestClient.put().uri("/case-notes/types/POM")
      .headers(addBearerToken(token))
      .bodyValue(
        """{
            "description": "Change The Desc","
            "active": notvalidtype
            }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().is5xxServerError

    // amend the case note type to description that is too long
    webTestClient.put().uri("/case-notes/types/POM")
      .headers(addBearerToken(token))
      .bodyValue(
        """{
            "description": "012345678901234567890123456789012345678901234567890123456789012345678901234567890",
            "active": true
            }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().json(readFile("validation-error2.json"))

    // try to add a new sub case note type that is too long
    webTestClient.post().uri("/case-notes/types/POM")
      .headers(addBearerToken(token))
      .bodyValue(
        """{
            "type": "TOOLONG1234567890",
            "description": "New Type",
            "active": false
            }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().isBadRequest

    // try to add a new sub case note type where description is too long
    webTestClient.post().uri("/case-notes/types/POM")
      .headers(addBearerToken(token))
      .bodyValue(
        """{
            "type": "NEWSUBTYPE1",
            "description": "012345678901234567890123456789012345678901234567890123456789012345678901234567890","active": false
            }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().isBadRequest

    // amend the case note sub type with description is too long
    webTestClient.put().uri("/case-notes/types/POM/GEN")
      .headers(addBearerToken(token))
      .bodyValue(
        """{
            "description": "012345678901234567890123456789012345678901234567890123456789012345678901234567890",
            "active": true
            }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun testCanSoftDeleteCaseNote() {
    oAuthApi.subGetUserDetails("DELETE_CASE_NOTE_USER")
    elite2Api.subGetOffender("A1234BA")
    val token = jwtHelper.createJwt("DELETE_CASE_NOTE_USER", roles = DELETE_CASENOTE_ROLES)

    val postResponse = webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234BA")
      .headers(addBearerToken(token))
      .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is a case note"))
      .exchange()
      .expectStatus().isCreated
      .returnResult(CaseNote::class.java)
    val id = postResponse.responseBody.blockFirst()!!.caseNoteId

    webTestClient.get().uri("/case-notes/{offenderIdentifier}/{caseNoteIdentifier}", "A1234BA", id)
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("A1234BA-single-casenote.json"))

    webTestClient.delete().uri("/case-notes/{offenderIdentifier}/{caseNoteId}", "A1234BA", id)
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isOk

    webTestClient.get().uri("/case-notes/{offenderIdentifier}/{caseNoteIdentifier}", "A1234BA", id)
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .json(
        "{" +
          "'status':404," +
          "'developerMessage':'Resource with id [$id] not found." +
          "'}",
      )
  }

  @Test
  fun testSoftDeleteCaseNoteUserDoesntHaveRole() {
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
    val token = jwtHelper.createJwt("SECURE_CASENOTE_USER", roles = CASENOTES_ROLES)

    webTestClient.delete()
      .uri("/case-notes/{offenderIdentifier}/{caseNoteId}", "Z1234ZZ", "231eb4ee-c06c-49a3-846c-1b542cc0ed6b")
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun testDeleteNOMISCaseNoteNotFound() {
    oAuthApi.subGetUserDetails("DELETE_CASE_NOTE_USER")
    val token = jwtHelper.createJwt("DELETE_CASE_NOTE_USER", roles = DELETE_CASENOTE_ROLES)

    webTestClient.delete().uri("/case-notes/{offenderIdentifier}/{caseNoteId}", "Z1234ZZ", "12345678")
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .json(
        "{" +
          "'status':400," +
          "'developerMessage':'Case note id not a sensitive case note, please delete through NOMIS'" +
          "}",
      )
  }

  @Test
  fun testSoftDeleteCaseNoteNotFound() {
    oAuthApi.subGetUserDetails("DELETE_CASE_NOTE_USER")
    val token = jwtHelper.createJwt("DELETE_CASE_NOTE_USER", roles = DELETE_CASENOTE_ROLES)

    webTestClient.delete()
      .uri("/case-notes/{offenderIdentifier}/{caseNoteId}", "Z1234ZZ", "231eb4ee-c06c-49a3-846c-1b542cc0ed6b")
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .json(
        "{" +
          "'status':404," +
          "'developerMessage':'Case note not found'" +
          "}",
      )
  }

  @Test
  fun testSoftDeleteCaseNoteIDAndOffenderIdNotLinked() {
    oAuthApi.subGetUserDetails("DELETE_CASE_NOTE_USER")
    elite2Api.subGetOffender("A1234BC")
    val token = jwtHelper.createJwt("DELETE_CASE_NOTE_USER", roles = DELETE_CASENOTE_ROLES)

    val postResponse = webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234BC")
      .headers(addBearerToken(token))
      .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is a case note"))
      .exchange()
      .expectStatus().isCreated
      .returnResult(CaseNote::class.java)
    val id = postResponse.responseBody.blockFirst()!!.caseNoteId

    webTestClient.delete().uri("/case-notes/{offenderIdentifier}/{caseNoteId}", "Z1234ZZ", id)
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .json(
        "{" +
          "'status\':400," +
          "'developerMessage':'case note id not connected with offenderIdentifier'" +
          "}",
      )
  }

  @Test
  fun testCanSoftDeleteCaseNoteAmendment() {
    oAuthApi.subGetUserDetails("DELETE_CASE_NOTE_USER")
    elite2Api.subGetOffender("A1234BC")
    val token =
      jwtHelper.createJwt("DELETE_CASE_NOTE_USER", roles = DELETE_CASENOTE_ROLES, scope = listOf("read", "write"))

    val postResponse = webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234BC")
      .headers(addBearerToken(token))
      .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is a case note"))
      .exchange()
      .expectStatus().isCreated
      .returnResult(CaseNote::class.java)
    val id = postResponse.responseBody.blockFirst()!!.caseNoteId

    webTestClient.put().uri("/case-notes/{offenderIdentifier}/{caseNoteId}", "A1234BC", id)
      .headers(addBearerToken(token))
      .bodyValue("""{ "text": "Amended case note" }""")
      .exchange()
      .expectStatus().isOk
      .expectBody().json(readFile("A1234BC-update-casenote.json"))

    val postResponseAmendment =
      webTestClient.get().uri("/case-notes/{offenderIdentifier}/{caseNoteIdentifier}", "A1234BC", id)
        .headers(addBearerToken(token))
        .exchange()
        .expectStatus().isOk
        .returnResult(CaseNote::class.java)
    val amendmentId = postResponseAmendment.responseBody.blockFirst()!!.amendments.get(0).caseNoteAmendmentId

    webTestClient.delete()
      .uri("/case-notes/amendment/{offenderIdentifier}/{caseNoteAmendmentId}", "A1234BC", amendmentId)
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isOk

    webTestClient.get().uri("/case-notes/{offenderIdentifier}/{caseNoteIdentifier}", "A1234BC", id)
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(readFile("A1234BC-deleted-amendment-casenote.json"))
  }

  @Test
  fun testSoftDeleteCaseNoteAmendmentUserDoesntHaveRole() {
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
    val token = jwtHelper.createJwt("SECURE_CASENOTE_USER", roles = CASENOTES_ROLES)

    webTestClient.delete().uri("/case-notes/amendment/{offenderIdentifier}/{caseNoteAmendmentId}", "Z1234ZZ", 1L)
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun testSoftDeleteCaseNoteAmendmentNotFound() {
    oAuthApi.subGetUserDetails("DELETE_CASE_NOTE_USER")
    val token = jwtHelper.createJwt("DELETE_CASE_NOTE_USER", roles = DELETE_CASENOTE_ROLES)

    webTestClient.delete().uri("/case-notes/amendment/{offenderIdentifier}/{caseNoteAmendmentId}", "Z1234ZZ", 4L)
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .json(
        "{" +
          "'status':404," +
          "'developerMessage':'Case note amendment not found'" +
          "}",
      )
  }

  @Test
  fun testSoftDeleteCaseNoteAmendmentIDAndOffenderIdNotLinked() {
    oAuthApi.subGetUserDetails("DELETE_CASE_NOTE_USER")
    elite2Api.subGetOffender("A1234BE")
    val token = jwtHelper.createJwt("DELETE_CASE_NOTE_USER", roles = DELETE_CASENOTE_ROLES)

    val postResponse = webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234BE")
      .headers(addBearerToken(token))
      .bodyValue(CREATE_CASE_NOTE_WITHOUT_LOC.format("This is a case note"))
      .exchange()
      .expectStatus().isCreated
      .returnResult(CaseNote::class.java)
    val id = postResponse.responseBody.blockFirst()!!.caseNoteId

    webTestClient.put().uri("/case-notes/{offenderIdentifier}/{caseNoteId}", "A1234BE", id)
      .headers(addBearerToken(token))
      .bodyValue("""{ "text": "Amended case note" }""")
      .exchange()
      .expectStatus().isOk

    val postResponseAmendment =
      webTestClient.get().uri("/case-notes/{offenderIdentifier}/{caseNoteIdentifier}", "A1234BE", id)
        .headers(addBearerToken(token))
        .exchange()
        .expectStatus().isOk
        .returnResult(CaseNote::class.java)
    val amendmentId = postResponseAmendment.responseBody.blockFirst()!!.amendments.get(0).caseNoteAmendmentId

    webTestClient.delete()
      .uri("/case-notes/amendment/{offenderIdentifier}/{caseNoteAmendmentId}", "Z1234ZZ", amendmentId)
      .headers(addBearerToken(token))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .json(
        "{" +
          "'status':400," +
          "'developerMessage':'case note amendment id not connected with offenderIdentifier'" +
          "}",
      )
  }

  companion object {
    private const val CREATE_CASE_NOTE =
      """{"locationId": "%s", "type": "POM", "subType": "GEN", "text": "%s"}"""
    private const val CREATE_CASE_NOTE_WITHOUT_LOC =
      """{"type": "POM", "subType": "GEN", "text": "%s"}"""
    private const val CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC =
      """{"type": "BOB", "subType": "SMITH", "text": "%s"}"""
    private const val CREATE_CASE_NOTE_BY_TYPE =
      """{"type": "%s", "subType": "%s", "text": "%s"}"""
    private val POM_ROLE = listOf("ROLE_POM")
    private val CASENOTES_ROLES = listOf("ROLE_VIEW_SENSITIVE_CASE_NOTES", "ROLE_ADD_SENSITIVE_CASE_NOTES")
    private val SYSTEM_ROLES = listOf("ROLE_SYSTEM_USER")
    private val DELETE_CASENOTE_ROLES = listOf(
      "ROLE_DELETE_SENSITIVE_CASE_NOTES",
      "ROLE_SYSTEM_USER",
      "ROLE_VIEW_SENSITIVE_CASE_NOTES",
      "ROLE_ADD_SENSITIVE_CASE_NOTES",
    )
  }
}
