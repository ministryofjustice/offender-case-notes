package uk.gov.justice.hmpps.casenotes.controllers;

import org.junit.jupiter.api.Test;
import uk.gov.justice.hmpps.casenotes.dto.CaseNote;
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension.elite2Api;
import static uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.oAuthApi;

public class CaseNoteResourceTest extends ResourceTest {

    private static final String CREATE_CASE_NOTE = "{\"locationId\": \"%s\", \"type\": \"POM\", \"subType\": \"GEN\", \"text\": \"%s\"}";
    private static final String CREATE_CASE_NOTE_WITHOUT_LOC = "{\"type\": \"POM\", \"subType\": \"GEN\", \"text\": \"%s\"}";
    private static final String CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC = "{\"type\": \"BOB\", \"subType\": \"SMITH\", \"text\": \"%s\"}";
    private static final String CREATE_CASE_NOTE_BY_TYPE = "{\"type\": \"%s\", \"subType\": \"%s\", \"text\": \"%s\"}";

    private static final List<String> POM_ROLE = List.of("ROLE_POM");
    private static final List<String> CASENOTES_ROLES = List.of("ROLE_VIEW_SENSITIVE_CASE_NOTES", "ROLE_ADD_SENSITIVE_CASE_NOTES");
    private static final List<String> SYSTEM_ROLES = List.of("ROLE_SYSTEM_USER");

    @Test
    public void testGetCaseNoteTypesNormal() {
        elite2Api.subGetCaseNoteTypes();

        webTestClient.get().uri("/case-notes/types")
                .headers(addBearerAuthorisation("API_TEST_USER", List.of()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("caseNoteTypes.json"));
    }

    @Test
    public void testGetCaseNoteTypesSecure() {
        elite2Api.subGetCaseNoteTypes();

        webTestClient.get().uri("/case-notes/types")
                .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("caseNoteTypesSecure.json"));
    }

    @Test
    public void testGetCaseNoteTypesSecurePomRole() {
        elite2Api.subGetCaseNoteTypes();

        webTestClient.get().uri("/case-notes/types")
                .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", POM_ROLE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("caseNoteTypesSecure.json"));
    }

    @Test
    public void testUserCaseNoteTypesNormal() {
        elite2Api.subUserCaseNoteTypes();

        webTestClient.get().uri("/case-notes/types-for-user")
                .headers(addBearerAuthorisation("API_TEST_USER", List.of()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("userCaseNoteTypes.json"));
    }

    @Test
    public void testUserCaseNoteTypesSecure() {
        elite2Api.subUserCaseNoteTypes();

        webTestClient.get().uri("/case-notes/types-for-user")
                .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("userCaseNoteTypesSecure.json"));
    }

    @Test
    public void testUserCaseNoteTypesSecurePomRole() {
        elite2Api.subUserCaseNoteTypes();

        webTestClient.get().uri("/case-notes/types-for-user")
                .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", POM_ROLE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("userCaseNoteTypesSecure.json"));
    }

    @Test
    public void testRetrieveCaseNotesForOffenderSensitive() {
        oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2Api.subGetOffender("A1234AA");
        elite2Api.subGetCaseNotesForOffender("A1234AA");

        final var token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES);

        webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AA")
                .headers(addBearerToken(token))
                .bodyValue(format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is a case note"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .json(readFile("A1234AA-create-casenote.json"));

        webTestClient.get().uri("/case-notes/{offenderIdentifier}", "A1234AA")
                .headers(addBearerToken(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("A1234AA-casenote.json"));
    }

    @Test
    public void testCanRetrieveCaseNotesForOffenderNormal() {
        oAuthApi.subGetUserDetails("API_TEST_USER");
        elite2Api.subGetCaseNotesForOffender("A1234AA");

        webTestClient.get().uri("/case-notes/{offenderIdentifier}", "A1234AA")
                .headers(addBearerAuthorisation("API_TEST_USER", List.of()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("A1234AA-normal-casenote.json"));
    }

    @Test
    public void testRetrieveCaseNotesWillReturn404IfOffenderNotFound() {
        oAuthApi.subGetUserDetails("API_TEST_USER");
        elite2Api.subGetCaseNotesForOffenderNotFound("A1234AA");

        webTestClient.get().uri("/case-notes/{offenderIdentifier}", "A1234AA")
                .headers(addBearerAuthorisation("API_TEST_USER", List.of()))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .json(readFile("offender-not-found.json"));
    }

    @Test
    public void testCanRetrieveCaseNoteForOffender() {
        oAuthApi.subGetUserDetails("API_TEST_USER");
        elite2Api.subGetOffender("A1234AA");
        elite2Api.subGetCaseNoteForOffender("A1234AA", 131232L);

        webTestClient.get().uri("/case-notes/{offenderIdentifier}/{caseNoteIdentifier}", "A1234AA", "131232")
                .headers(addBearerAuthorisation("API_TEST_USER", List.of()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("A1234AA-single-normal-casenote.json"));
    }

    @Test
    public void testRetrieveCaseNoteForOffenderSensitive() {
        oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2Api.subGetOffender("A1234AF");

        final var token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES);

        final var postResponse = webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AF")
                .headers(addBearerToken(token))
                .bodyValue(format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is a case note"))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(CaseNote.class);
        final var id = Objects.requireNonNull(postResponse.getResponseBody().blockFirst()).getCaseNoteId();

        webTestClient.get().uri("/case-notes/{offenderIdentifier}/{caseNoteIdentifier}", "A1234AF", id)
                .headers(addBearerToken(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("A1234AF-single-casenote.json"));
    }

    @Test
    public void testCanCreateCaseNote_Normal() {
        oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2Api.subCreateCaseNote("A1234AE");

        // create the case note
        webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AE")
                .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
                .bodyValue(format(CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC, "This is another case note"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .json(readFile("A1234AE-create-casenote.json"));
    }

    @Test
    public void testCanCreateCaseNote_Secure() {
        oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2Api.subGetOffender("A1234AD");

        // create the case note
        webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AD")
                .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
                .bodyValue(format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is another case note"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .json(readFile("A1234AD-create-casenote.json"));
    }

    @Test
    public void testCanCreateCaseNote_SecureWithPomRole() {
        oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2Api.subGetOffender("A1234AD");

        // create the case note
        webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AD")
                .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", POM_ROLE))
                .bodyValue(format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is another case note"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .json(readFile("A1234AD-create-casenote.json"));
    }

    @Test
    public void testCannotCreateInactiveCaseNote_Secure() {
        oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2Api.subGetOffender("A1234AD");

        // create the case note
        webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AD")
                .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
                .bodyValue(format(CREATE_CASE_NOTE_BY_TYPE, "OLDPOM", "OLDTWO", "This is another case note with inactive case note type"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testCanCreateAmendments() {
        oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2Api.subGetOffender("A1234AB");
        elite2Api.subGetCaseNotesForOffender("A1234AB");

        final var token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES);

        // create the case note
        final var postResponse = webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AB")
                .headers(addBearerToken(token))
                .bodyValue(format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is another case note"))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(CaseNote.class);

        // amend the case note
        webTestClient.put().uri("/case-notes/{offenderIdentifier}/{caseNoteId}",
                "A1234AB", Objects.requireNonNull(postResponse.getResponseBody().blockFirst()).getCaseNoteId())
                .headers(addBearerToken(token))
                .bodyValue("{ \"text\": \"Amended case note\" }")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(readFile("A1234AB-update-casenote.json"));

        // check the case note now correct
        webTestClient.get().uri("/case-notes/{offenderIdentifier}", "A1234AB")
                .headers(addBearerToken(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("A1234AB-casenote.json"));
    }

    @Test
    public void testCanCreateAmendments_Normal() {
        oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2Api.subAmendCaseNote("A1234AE", "12345");

        // amend the case note
        webTestClient.put().uri("/case-notes/{offenderIdentifier}/{caseNoteId}", "A1234AE", "12345")
                .headers(addBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES))
                .bodyValue("{ \"text\": \"Amended case note\" }")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("A1234AE-create-casenote.json"));
    }

    @Test
    public void testCanFilterCaseNotes() {
        oAuthApi.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2Api.subGetOffender("A1234AC");
        elite2Api.subGetCaseNotesForOffender("A1234AC");

        final var token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES);

        webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AC")
                .headers(addBearerToken(token))
                .bodyValue(format(CREATE_CASE_NOTE, "MDI", "This is a case note 1"))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AC")
                .headers(addBearerToken(token))
                .bodyValue(format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is a case note 2"))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/case-notes/{offenderIdentifier}", "A1234AC")
                .headers(addBearerToken(token))
                .bodyValue(format(CREATE_CASE_NOTE, "LEI", "This is a case note 3"))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.get().uri("/case-notes/{offenderIdentifier}?size={size}&page={page}", "A1234AC", "2", "1")
                .headers(addBearerToken(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("A1234AC-casenote.json"));
    }

    @Test
    public void testCanCreateAndUpdateTypes() {
        oAuthApi.subGetUserDetails("SYSTEM_USER_READ_WRITE");

        final var token = createJwt("SYSTEM_USER_READ_WRITE", SYSTEM_ROLES);

        // add a new case note parent type called NEWTYPE1
        webTestClient.post().uri("/case-notes/types")
                .headers(addBearerToken(token))
                .bodyValue("{" +
                        "\"type\": \"NEWTYPE1\",\n" +
                        "\"description\": \"A New Type 1\"," +
                        "\"active\": false" +
                        "}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .json(readFile("newCaseNoteType1.json"));

        // amend the case note type from inactive to active and change description
        webTestClient.put().uri("/case-notes/types/NEWTYPE1")
                .headers(addBearerToken(token))
                .bodyValue("{" +
                        "\"description\": \"Change The Desc\"," +
                        "\"active\": true" +
                        "}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("updateCaseNoteType1.json"));

        // add a new sub case note type called NEWSUBTYPE1
        webTestClient.post().uri("/case-notes/types/NEWTYPE1")
                .headers(addBearerToken(token))
                .bodyValue("{" +
                        "\"type\": \"NEWSUBTYPE1\",\n" +
                        "\"description\": \"A New Sub Type 1\"," +
                        "\"active\": false" +
                        "}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .json(readFile("newCaseNoteSubType1.json"));

        // amend the case note sub type to active and new description
        webTestClient.put().uri("/case-notes/types/NEWTYPE1/NEWSUBTYPE1")
                .headers(addBearerToken(token))
                .bodyValue("{" +
                        "\"description\": \"Change The Desc\"," +
                        "\"active\": true" +
                        "}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(readFile("updateCaseNoteSubType1.json"));
    }

    @Test
    public void testCannotCreateAndUpdateTypesWhenInvalid() {
        OAuthExtension.oAuthApi.subGetUserDetails("SYSTEM_USER_READ_WRITE");

        final var token = createJwt("SYSTEM_USER_READ_WRITE", SYSTEM_ROLES);

        // add a new case note parent type called TOOLONG1234567890 that is more than 12 chars
        webTestClient.post().uri("/case-notes/types")
                .headers(addBearerToken(token))
                .bodyValue("{" +
                        "\"type\": \"TOOLONG1234567890\",\n" +
                        "\"description\": \"Wrong!\"," +
                        "\"active\": false" +
                        "}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(readFile("validation-error1.json"));

        // amend the case note type and use an invalid boolean value
        webTestClient.put().uri("/case-notes/types/POM")
                .headers(addBearerToken(token))
                .bodyValue("{" +
                        "\"description\": \"Change The Desc\"," +
                        "\"active\": notvalidtype" +
                        "}")
                .exchange()
                .expectStatus().is5xxServerError();

        // amend the case note type to description that is too long
        webTestClient.put().uri("/case-notes/types/POM")
                .headers(addBearerToken(token))
                .bodyValue("{" +
                        "\"description\": \"012345678901234567890123456789012345678901234567890123456789012345678901234567890\"," +
                        "\"active\": true" +
                        "}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(readFile("validation-error2.json"));

        // try to add a new sub case note type that is too long
        webTestClient.post().uri("/case-notes/types/POM")
                .headers(addBearerToken(token))
                .bodyValue("{" +
                        "\"type\": \"TOOLONG1234567890\",\n" +
                        "\"description\": \"New Type\"," +
                        "\"active\": false" +
                        "}")
                .exchange()
                .expectStatus().isBadRequest();

        // try to add a new sub case note type where description is too long
        webTestClient.post().uri("/case-notes/types/POM")
                .headers(addBearerToken(token))
                .bodyValue("{" +
                        "\"type\": \"NEWSUBTYPE1\",\n" +
                        "\"description\": \"012345678901234567890123456789012345678901234567890123456789012345678901234567890\"," +
                        "\"active\": false" +
                        "}")
                .exchange()
                .expectStatus().isBadRequest();

        // amend the case note sub type with description is too long
        webTestClient.put().uri("/case-notes/types/POM/GEN")
                .headers(addBearerToken(token))
                .bodyValue("{" +
                        "\"description\": \"012345678901234567890123456789012345678901234567890123456789012345678901234567890\"," +
                        "\"active\": true" +
                        "}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
