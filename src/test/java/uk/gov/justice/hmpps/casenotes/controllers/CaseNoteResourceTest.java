package uk.gov.justice.hmpps.casenotes.controllers;

import org.junit.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import uk.gov.justice.hmpps.casenotes.dto.CaseNote;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class CaseNoteResourceTest extends ResourceTest {

    private static final String CREATE_CASE_NOTE = "{\"locationId\": \"%s\", \"type\": \"POM\", \"subType\": \"GEN\", \"text\": \"%s\"}";
    private static final String CREATE_CASE_NOTE_WITHOUT_LOC = "{\"type\": \"POM\", \"subType\": \"GEN\", \"text\": \"%s\"}";
    private static final String CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC = "{\"type\": \"BOB\", \"subType\": \"SMITH\", \"text\": \"%s\"}";
    private static final String CREATE_CASE_NOTE_BY_TYPE = "{\"type\": \"%s\", \"subType\": \"%s\", \"text\": \"%s\"}";

    private static final List<String> CASENOTES_ROLES = List.of("ROLE_VIEW_SENSITIVE_CASE_NOTES", "ROLE_ADD_SENSITIVE_CASE_NOTES");
    private static final List<String> SYSTEM_ROLES = List.of("ROLE_SYSTEM_USER");

    @Test
    public void testGetCaseNoteTypesNormal() {
        elite2MockServer.subGetCaseNoteTypes();

        final var response = testRestTemplate.exchange(
                "/case-notes/types",
                HttpMethod.GET,
                createHttpEntityWithBearerAuthorisation("API_TEST_USER", List.of()),
                new ParameterizedTypeReference<String>() {
                });

        assertThatJsonFileAndStatus(response, 200, "caseNoteTypes.json");
    }

    @Test
    public void testGetCaseNoteTypesSecure() {
        elite2MockServer.subGetCaseNoteTypes();

        final var response = testRestTemplate.exchange(
                "/case-notes/types",
                HttpMethod.GET,
                createHttpEntityWithBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES),
                new ParameterizedTypeReference<String>() {
                });

        assertThatJsonFileAndStatus(response, 200, "caseNoteTypesSecure.json");
    }

    @Test
    public void testUserCaseNoteTypesNormal() {
        elite2MockServer.subUserCaseNoteTypes();

        final var response = testRestTemplate.exchange(
                "/case-notes/types-for-user",
                HttpMethod.GET,
                createHttpEntityWithBearerAuthorisation("API_TEST_USER", List.of()),
                new ParameterizedTypeReference<String>() {
                });

        assertThatJsonFileAndStatus(response, 200, "userCaseNoteTypes.json");
    }

    @Test
    public void testUserCaseNoteTypesSecure() {
        elite2MockServer.subUserCaseNoteTypes();

        final var response = testRestTemplate.exchange(
                "/case-notes/types-for-user",
                HttpMethod.GET,
                createHttpEntityWithBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES),
                new ParameterizedTypeReference<String>() {
                });

        assertThatJsonFileAndStatus(response, 200, "userCaseNoteTypesSecure.json");
    }

    @Test
    public void testRetrieveCaseNotesForOffenderSensitive() {
        oauthMockServer.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2MockServer.subGetOffender("A1234AA");
        elite2MockServer.subGetCaseNotesForOffender("A1234AA");

        final var token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES);

        final var postResponse = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntity(token, format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is a case note")),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AA");

        assertThatJsonFileAndStatus(postResponse, 201, "A1234AA-create-casenote.json");

        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AA");

        assertThatJsonFileAndStatus(response, 200, "A1234AA-casenote.json");
    }

    @Test
    public void testCanRetrieveCaseNotesForOffenderNormal() {
        oauthMockServer.subGetUserDetails("API_TEST_USER");
        elite2MockServer.subGetOffender("A1234AA");
        elite2MockServer.subGetCaseNotesForOffender("A1234AA");

        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.GET,
                createHttpEntityWithBearerAuthorisation("API_TEST_USER", List.of()),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AA");

        assertThatJsonFileAndStatus(response, 200, "A1234AA-normal-casenote.json");
    }

    @Test
    public void testCanRetrieveCaseNoteForOffender() {
        oauthMockServer.subGetUserDetails("API_TEST_USER");
        elite2MockServer.subGetOffender("A1234AA");
        elite2MockServer.subGetCaseNoteForOffender("A1234AA", 131232L);

        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}/{caseNoteIdentifier}",
                HttpMethod.GET,
                createHttpEntityWithBearerAuthorisation("API_TEST_USER", List.of()),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AA", "131232");

        assertThatJsonFileAndStatus(response, 200, "A1234AA-single-normal-casenote.json");
    }

    @Test
    public void testRetrieveCaseNoteForOffenderSensitive() {
        oauthMockServer.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2MockServer.subGetOffender("A1234AF");

        final var token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES);

        final var postResponse = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntity(token, format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is a case note")),
                new ParameterizedTypeReference<CaseNote>() {
                },
                "A1234AF");
        final var id = Objects.requireNonNull(postResponse.getBody()).getCaseNoteId();

        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}/{caseNoteIdentifier}",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AF", id);

        assertThatJsonFileAndStatus(response, 200, "A1234AF-single-casenote.json");
    }

    @Test
    public void testCanCreateCaseNote_Normal() {
        oauthMockServer.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2MockServer.subCreateCaseNote("A1234AE");

        // create the case note
        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntityWithBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES, format(CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC, "This is another case note")),
                String.class,
                "A1234AE");

        assertThatJsonFileAndStatus(response, 201, "A1234AE-create-casenote.json");
    }

    @Test
    public void testCanCreateCaseNote_Secure() {
        oauthMockServer.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2MockServer.subGetOffender("A1234AD");

        // create the case note
        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntityWithBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES, format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is another case note")),
                String.class,
                "A1234AD");

        assertThatJsonFileAndStatus(response, 201, "A1234AD-create-casenote.json");
    }

    @Test
    public void testCannotCreateInactiveCaseNote_Secure() {
        oauthMockServer.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2MockServer.subGetOffender("A1234AD");

        // create the case note
        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntityWithBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES, format(CREATE_CASE_NOTE_BY_TYPE, "OLDPOM", "OLDTWO", "This is another case note with inactive case note type")),
                String.class,
                "A1234AD");

        assertThatStatus(response, 400);
    }

    @Test
    public void testCanCreateAmendments() {
        oauthMockServer.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2MockServer.subGetOffender("A1234AB");
        elite2MockServer.subGetCaseNotesForOffender("A1234AB");

        final var token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES);

        // create the case note
        final var postResponse = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntity(token, format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is another case note")),
                new ParameterizedTypeReference<CaseNote>() {
                },
                "A1234AB");

        assertThat(postResponse.getStatusCodeValue()).isEqualTo(201);

        // amend the case note
        final var postAmendResponse = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}/{caseNoteId}",
                HttpMethod.PUT,
                createHttpEntity(token, "{ \"text\": \"Amended case note\" }"),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AB", Objects.requireNonNull(postResponse.getBody()).getCaseNoteId());

        assertThatJsonFileAndStatus(postAmendResponse, 200, "A1234AB-update-casenote.json");

        // check the case note now correct
        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AB");

        assertThatJsonFileAndStatus(response, 200, "A1234AB-casenote.json");
    }

    @Test
    public void testCanCreateAmendments_Normal() {
        oauthMockServer.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2MockServer.subAmendCaseNote("A1234AE", "12345");

        // amend the case note
        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}/{caseNoteId}",
                HttpMethod.PUT,
                createHttpEntityWithBearerAuthorisation("SECURE_CASENOTE_USER", CASENOTES_ROLES, "{ \"text\": \"Amended case note\" }"),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AE", "12345");

        assertThatJsonFileAndStatus(response, 200, "A1234AE-create-casenote.json");
    }

    @Test
    public void testCanFilterCaseNotes() {
        oauthMockServer.subGetUserDetails("SECURE_CASENOTE_USER");
        elite2MockServer.subGetOffender("A1234AC");
        elite2MockServer.subGetCaseNotesForOffender("A1234AC");

        final var token = createJwt("SECURE_CASENOTE_USER", CASENOTES_ROLES);

        testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntity(token, format(CREATE_CASE_NOTE, "MDI", "This is a case note 1")),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AC");

        testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntity(token, format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is a case note 2")),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AC");

        testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntity(token, format(CREATE_CASE_NOTE, "LEI", "This is a case note 3")),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AC");

        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}?size={size}&page={page}",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AC", "2", "1");

        assertThatJsonFileAndStatus(response, 200, "A1234AC-casenote.json");
    }

    @Test
    public void testCanCreateAndUpdateTypes() {
        oauthMockServer.subGetUserDetails("SYSTEM_USER_READ_WRITE");

        final var token = createJwt("SYSTEM_USER_READ_WRITE", SYSTEM_ROLES);

        // add a new case note parent type called NEWTYPE1
        final var response = testRestTemplate.exchange(
                "/case-notes/types",
                HttpMethod.POST,
                createHttpEntity(token, "{" +
                        "\"type\": \"NEWTYPE1\",\n" +
                        "\"description\": \"A New Type 1\"," +
                        "\"active\": false" +
                        "}"),
                new ParameterizedTypeReference<String>() {
                });

        assertThatJsonFileAndStatus(response, 201, "newCaseNoteType1.json");

        // amend the case note type from inactive to active and change description
        final var responseUpdate = testRestTemplate.exchange(
                "/case-notes/types/NEWTYPE1",
                HttpMethod.PUT,
                createHttpEntity(token, "{" +
                        "\"description\": \"Change The Desc\"," +
                        "\"active\": true" +
                        "}"),
                new ParameterizedTypeReference<String>() {
                });

        assertThatJsonFileAndStatus(responseUpdate, 200, "updateCaseNoteType1.json");

        // add a new sub case note type called NEWSUBTYPE1
        final var responseSubType = testRestTemplate.exchange(
                "/case-notes/types/NEWTYPE1",
                HttpMethod.POST,
                createHttpEntity(token, "{" +
                        "\"type\": \"NEWSUBTYPE1\",\n" +
                        "\"description\": \"A New Sub Type 1\"," +
                        "\"active\": false" +
                        "}"),
                new ParameterizedTypeReference<String>() {
                });

        assertThatJsonFileAndStatus(responseSubType, 201, "newCaseNoteSubType1.json");

        // amend the case note sub type to active and new description
        final var responseUpdateSubType = testRestTemplate.exchange(
                "/case-notes/types/NEWTYPE1/NEWSUBTYPE1",
                HttpMethod.PUT,
                createHttpEntity(token, "{" +
                        "\"description\": \"Change The Desc\"," +
                        "\"active\": true" +
                        "}"),
                new ParameterizedTypeReference<String>() {
                });

        assertThatJsonFileAndStatus(responseUpdateSubType, 200, "updateCaseNoteSubType1.json");
    }

    @Test
    public void testCannotCreateAndUpdateTypesWhenInvalid() {
        oauthMockServer.subGetUserDetails("SYSTEM_USER_READ_WRITE");

        final var token = createJwt("SYSTEM_USER_READ_WRITE", SYSTEM_ROLES);

        // add a new case note parent type called TOOLONG1234567890 that is more than 12 chars
        final var response = testRestTemplate.exchange(
                "/case-notes/types",
                HttpMethod.POST,
                createHttpEntity(token, "{" +
                        "\"type\": \"TOOLONG1234567890\",\n" +
                        "\"description\": \"Wrong!\"," +
                        "\"active\": false" +
                        "}"),
                new ParameterizedTypeReference<String>() {
                });

        assertThatJsonFileAndStatus(response, 400, "validation-error1.json");

        // amend the case note type and use an invalid boolean value
        final var responseUpdate = testRestTemplate.exchange(
                "/case-notes/types/POM",
                HttpMethod.PUT,
                createHttpEntity(token, "{" +
                        "\"description\": \"Change The Desc\"," +
                        "\"active\": notvalidtype" +
                        "}"),
                new ParameterizedTypeReference<String>() {
                });

        assertThatStatus(responseUpdate, 500);

        // amend the case note type to description that is too long
        final var responseUpdate2 = testRestTemplate.exchange(
                "/case-notes/types/POM",
                HttpMethod.PUT,
                createHttpEntity(token, "{" +
                        "\"description\": \"012345678901234567890123456789012345678901234567890123456789012345678901234567890\"," +
                        "\"active\": true" +
                        "}"),
                new ParameterizedTypeReference<String>() {
                });

        assertThatJsonFileAndStatus(responseUpdate2, 400, "validation-error2.json");

        // try to add a new sub case note type that is too long
        final var responseSubType = testRestTemplate.exchange(
                "/case-notes/types/POM",
                HttpMethod.POST,
                createHttpEntity(token, "{" +
                        "\"type\": \"TOOLONG1234567890\",\n" +
                        "\"description\": \"New Type\"," +
                        "\"active\": false" +
                        "}"),
                new ParameterizedTypeReference<String>() {
                });

        assertThatStatus(responseSubType, 400);

        // try to add a new sub case note type where description is too long
        final var responseSubType2 = testRestTemplate.exchange(
                "/case-notes/types/POM",
                HttpMethod.POST,
                createHttpEntity(token, "{" +
                        "\"type\": \"NEWSUBTYPE1\",\n" +
                        "\"description\": \"012345678901234567890123456789012345678901234567890123456789012345678901234567890\"," +
                        "\"active\": false" +
                        "}"),
                new ParameterizedTypeReference<String>() {
                });

        assertThatStatus(responseSubType2, 400);

        // amend the case note sub type with description is too long
        final var responseUpdateSubType = testRestTemplate.exchange(
                "/case-notes/types/POM/GEN",
                HttpMethod.PUT,
                createHttpEntity(token, "{" +
                        "\"description\": \"012345678901234567890123456789012345678901234567890123456789012345678901234567890\"," +
                        "\"active\": true" +
                        "}"),
                new ParameterizedTypeReference<String>() {
                });

        assertThatStatus(responseUpdateSubType, 400);
    }

}
