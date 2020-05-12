package uk.gov.justice.hmpps.casenotes.controllers

import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension.Companion.elite2Api
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi
import java.util.*

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

  @Test
  fun testRetrieveCaseNotesForOffenderSensitive() {
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
    elite2Api.subGetOffender("A1234AA")
    elite2Api.subGetCaseNotesForOffender("A1234AA")
    val token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES)
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AA")
        .headers(addBearerToken(token))
        .bodyValue(String.format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is a case note"))
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
    val token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES)
    val postResponse = webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AF")
        .headers(addBearerToken(token))
        .bodyValue(String.format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is a case note"))
        .exchange()
        .expectStatus().isCreated
        .returnResult(CaseNote::class.java)
    val id = Objects.requireNonNull(postResponse.responseBody.blockFirst()).caseNoteId
    webTestClient.get().uri("/case-notes/{offenderIdentifier}/{caseNoteIdentifier}", "A1234AF", id)
        .headers(addBearerToken(token))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(readFile("A1234AF-single-casenote.json"))
  }

  @Test
  fun testCanCreateCaseNote_Normal() {
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
    elite2Api.subCreateCaseNote("A1234AE")

    // create the case note
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AE")
        .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
        .bodyValue(String.format(CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC, "This is another case note"))
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
        .bodyValue(String.format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is another case note"))
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
        .bodyValue(String.format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is another case note"))
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
        .bodyValue(String.format(CREATE_CASE_NOTE_BY_TYPE, "OLDPOM", "OLDTWO", "This is another case note with inactive case note type"))
        .exchange()
        .expectStatus().isBadRequest
  }

  @Test
  fun testCanCreateAmendments() {
    oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER")
    elite2Api.subGetOffender("A1234AB")
    elite2Api.subGetCaseNotesForOffender("A1234AB")
    val token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES)

    // create the case note
    val postResponse = webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AB")
        .headers(addBearerToken(token))
        .bodyValue(String.format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is another case note"))
        .exchange()
        .expectStatus().isCreated
        .returnResult(CaseNote::class.java)

    // amend the case note
    webTestClient.put().uri("/case-notes/{offenderIdentifier}/{caseNoteId}",
        "A1234AB", Objects.requireNonNull(postResponse.responseBody.blockFirst()).caseNoteId)
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
    val token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES)
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AC")
        .headers(addBearerToken(token))
        .bodyValue(String.format(CREATE_CASE_NOTE, "MDI", "This is a case note 1"))
        .exchange()
        .expectStatus().isCreated
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AC")
        .headers(addBearerToken(token))
        .bodyValue(String.format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is a case note 2"))
        .exchange()
        .expectStatus().isCreated
    webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AC")
        .headers(addBearerToken(token))
        .bodyValue(String.format(CREATE_CASE_NOTE, "LEI", "This is a case note 3"))
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
    val token = createJwt("SYSTEM_USER_READ_WRITE", SYSTEM_ROLES)

    // add a new case note parent type called NEWTYPE1
    webTestClient.post().uri("/case-notes/types")
        .headers(addBearerToken(token))
        .bodyValue("""{
            "type": "NEWTYPE1",
            "description": "A New Type 1",
            "active": false
            }""".trimIndent())
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .json(readFile("newCaseNoteType1.json"))

    // amend the case note type from inactive to active and change description
    webTestClient.put().uri("/case-notes/types/NEWTYPE1")
        .headers(addBearerToken(token))
        .bodyValue("""{ 
            "description": "Change The Desc",
            "active": true
            }""".trimIndent())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(readFile("updateCaseNoteType1.json"))

    // add a new sub case note type called NEWSUBTYPE1
    webTestClient.post().uri("/case-notes/types/NEWTYPE1")
        .headers(addBearerToken(token))
        .bodyValue("""{
            "type": "NEWSUBTYPE1",
            "description": "A New Sub Type 1",
            "active": false
            }""".trimIndent())
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .json(readFile("newCaseNoteSubType1.json"))

    // amend the case note sub type to active and new description
    webTestClient.put().uri("/case-notes/types/NEWTYPE1/NEWSUBTYPE1")
        .headers(addBearerToken(token))
        .bodyValue("""{
            "description": "Change The Desc",
            "active": true
            }""".trimIndent())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(readFile("updateCaseNoteSubType1.json"))
  }

  @Test
  fun testCannotCreateAndUpdateTypesWhenInvalid() {
    oAuthApi.subGetUserDetails("SYSTEM_USER_READ_WRITE")
    val token = createJwt("SYSTEM_USER_READ_WRITE", SYSTEM_ROLES)

    // add a new case note parent type called TOOLONG1234567890 that is more than 12 chars
    webTestClient.post().uri("/case-notes/types")
        .headers(addBearerToken(token))
        .bodyValue("""{
            "type": "TOOLONG1234567890",
            "description": "Wrong!",
            "active": false
            }""".trimIndent())
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().json(readFile("validation-error1.json"))

    // amend the case note type and use an invalid boolean value
    webTestClient.put().uri("/case-notes/types/POM")
        .headers(addBearerToken(token))
        .bodyValue("""{
            "description": "Change The Desc","
            "active": notvalidtype
            }""".trimIndent())
        .exchange()
        .expectStatus().is5xxServerError

    // amend the case note type to description that is too long
    webTestClient.put().uri("/case-notes/types/POM")
        .headers(addBearerToken(token))
        .bodyValue("""{
            "description": "012345678901234567890123456789012345678901234567890123456789012345678901234567890",
            "active": true
            }""".trimIndent())
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().json(readFile("validation-error2.json"))

    // try to add a new sub case note type that is too long
    webTestClient.post().uri("/case-notes/types/POM")
        .headers(addBearerToken(token))
        .bodyValue("""{
            "type": "TOOLONG1234567890",
            "description": "New Type",
            "active": false
            }""".trimIndent())
        .exchange()
        .expectStatus().isBadRequest

    // try to add a new sub case note type where description is too long
    webTestClient.post().uri("/case-notes/types/POM")
        .headers(addBearerToken(token))
        .bodyValue("""{
            "type": "NEWSUBTYPE1",
            "description": "012345678901234567890123456789012345678901234567890123456789012345678901234567890","active": false
            }""".trimIndent())
        .exchange()
        .expectStatus().isBadRequest

    // amend the case note sub type with description is too long
    webTestClient.put().uri("/case-notes/types/POM/GEN")
        .headers(addBearerToken(token))
        .bodyValue("""{
            "description": "012345678901234567890123456789012345678901234567890123456789012345678901234567890",
            "active": true
            }""".trimIndent())
        .exchange()
        .expectStatus().isBadRequest
  }

  companion object {
    private const val CREATE_CASE_NOTE = """{"locationId": "%s", "type": "POM", "subType": "GEN", "text": "%s"}"""
    private const val CREATE_CASE_NOTE_WITHOUT_LOC = """{"type": "POM", "subType": "GEN", "text": "%s"}"""
    private const val CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC = """{"type": "BOB", "subType": "SMITH", "text": "%s"}"""
    private const val CREATE_CASE_NOTE_BY_TYPE = """{"type": "%s", "subType": "%s", "text": "%s"}"""
    private val POM_ROLE = listOf("ROLE_POM")
    private val CASENOTES_ROLES = listOf("ROLE_VIEW_SENSITIVE_CASE_NOTES", "ROLE_ADD_SENSITIVE_CASE_NOTES")
    private val SYSTEM_ROLES = listOf("ROLE_SYSTEM_USER")
  }
}
