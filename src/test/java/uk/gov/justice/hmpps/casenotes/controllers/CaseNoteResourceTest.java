package uk.gov.justice.hmpps.casenotes.controllers;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import uk.gov.justice.hmpps.casenotes.dto.CaseNote;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteCount;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteType;
import uk.gov.justice.hmpps.casenotes.utils.AuthTokenHelper;

import java.util.Objects;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.casenotes.utils.AuthTokenHelper.AuthToken.*;

public class CaseNoteResourceTest extends ResourceTest {

    private static final String CREATE_CASE_NOTE = "{\"locationId\": \"%s\", \"type\": \"POM\", \"subType\": \"GEN\", \"text\": \"%s\"}";
    private static final String CREATE_CASE_NOTE_WITHOUT_LOC = "{\"type\": \"POM\", \"subType\": \"GEN\", \"text\": \"%s\"}";
    private static final String CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC = "{\"type\": \"BOB\", \"subType\": \"SMITH\", \"text\": \"%s\"}";

    @Autowired
    private AuthTokenHelper authTokenHelper;

    @Test
    public void testGetCaseNoteTypesNormal() {
        elite2MockServer.subGetCaseNoteTypes();

        final var token = authTokenHelper.getToken(API_TEST_USER);

        final var response = testRestTemplate.exchange(
                "/case-notes/types",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                });

        assertJsonAndStatus(response, CaseNoteType.class, 200, "caseNoteTypes.json");
    }

    @Test
    public void testGetCaseNoteCountNormal() {
        elite2MockServer.subGetCaseNoteCount();

        final var token = authTokenHelper.getToken(API_TEST_USER);

        final var response = testRestTemplate.exchange(
                "/case-notes/{bookingId}/{type}/{subtype}/count?fromDate={fromDate}&toDate={toDate}",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                }, "1234567", "NEG", "IEP_WARN", "2019-05-30", "2019-08-30");

        assertJsonAndStatus(response, CaseNoteCount.class, 200, "caseNoteCount.json");
    }

    @Test
    public void testGetCaseNoteTypesSecure() {
        elite2MockServer.subGetCaseNoteTypes();

        final var token = authTokenHelper.getToken(SECURE_CASENOTE_USER);

        final var response = testRestTemplate.exchange(
                "/case-notes/types",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                });

        assertJsonAndStatus(response, CaseNoteType.class, 200, "caseNoteTypesSecure.json");
    }

    @Test
    public void testUserCaseNoteTypesNormal() {
        elite2MockServer.subUserCaseNoteTypes();

        final var token = authTokenHelper.getToken(API_TEST_USER);

        final var response = testRestTemplate.exchange(
                "/case-notes/types-for-user",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                });

        assertJsonAndStatus(response, CaseNoteType.class, 200, "userCaseNoteTypes.json");
    }

    @Test
    public void testUserCaseNoteTypesSecure() {
        elite2MockServer.subUserCaseNoteTypes();

        final var token = authTokenHelper.getToken(SECURE_CASENOTE_USER);

        final var response = testRestTemplate.exchange(
                "/case-notes/types-for-user",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                });

        assertJsonAndStatus(response, CaseNoteType.class, 200, "userCaseNoteTypesSecure.json");
    }

    @Test
    public void testRetrieveCaseNotesForOffenderSensitive() {
        oauthMockServer.subGetUserDetails(SECURE_CASENOTE_USER);
        elite2MockServer.subGetOffender("A1234AA");
        elite2MockServer.subGetCaseNotesForOffender("A1234AA");

        final var token = authTokenHelper.getToken(SECURE_CASENOTE_USER);

        final var postResponse = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntity(token, format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is a case note")),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AA");

        assertJsonAndStatus(postResponse, CaseNote.class, 201, "A1234AA-create-casenote.json");

        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AA");

        assertJsonAndStatus(response, CaseNote.class, 200, "A1234AA-casenote.json");
    }

    @Test
    public void testCanRetrieveCaseNotesForOffenderNormal() {
        oauthMockServer.subGetUserDetails(API_TEST_USER);
        elite2MockServer.subGetOffender("A1234AA");
        elite2MockServer.subGetCaseNotesForOffender("A1234AA");

        final var token = authTokenHelper.getToken(API_TEST_USER);

        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AA");

        assertJsonAndStatus(response, CaseNote.class, 200, "A1234AA-normal-casenote.json");
    }

    @Test
    public void testCanRetrieveCaseNoteForOffender() {
        oauthMockServer.subGetUserDetails(API_TEST_USER);
        elite2MockServer.subGetOffender("A1234AA");
        elite2MockServer.subGetCaseNoteForOffender("A1234AA", 131232L);

        final var token = authTokenHelper.getToken(API_TEST_USER);

        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}/{caseNoteIdentifier}",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AA", "131232");

        assertJsonAndStatus(response, CaseNote.class, 200, "A1234AA-single-normal-casenote.json");
    }

    @Test
    public void testRetrieveCaseNoteForOffenderSensitive() {
        oauthMockServer.subGetUserDetails(SECURE_CASENOTE_USER);
        elite2MockServer.subGetOffender("A1234AF");

        final var token = authTokenHelper.getToken(SECURE_CASENOTE_USER);

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

        assertJsonAndStatus(response, CaseNote.class, 200, "A1234AF-single-casenote.json");
    }

    @Test
    public void testCanCreateCaseNote_Normal() {
        oauthMockServer.subGetUserDetails(SECURE_CASENOTE_USER);
        elite2MockServer.subCreateCaseNote("A1234AE");

        final var token = authTokenHelper.getToken(SECURE_CASENOTE_USER);

        // create the case note
        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntity(token, format(CREATE_NORMAL_CASE_NOTE_WITHOUT_LOC, "This is another case note")),
                String.class,
                "A1234AE");

        assertJsonAndStatus(response, CaseNote.class, 201, "A1234AE-create-casenote.json");
    }

    @Test
    public void testCanCreateCaseNote_Secure() {
        oauthMockServer.subGetUserDetails(SECURE_CASENOTE_USER);
        elite2MockServer.subGetOffender("A1234AD");

        final var token = authTokenHelper.getToken(SECURE_CASENOTE_USER);

        // create the case note
        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntity(token, format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is another case note")),
                String.class,
                "A1234AD");

        assertJsonAndStatus(response, CaseNote.class, 201, "A1234AD-create-casenote.json");
    }

    @Test
    public void testCanCreateAmendments() {
        oauthMockServer.subGetUserDetails(SECURE_CASENOTE_USER);
        elite2MockServer.subGetOffender("A1234AB");
        elite2MockServer.subGetCaseNotesForOffender("A1234AB");

        final var token = authTokenHelper.getToken(SECURE_CASENOTE_USER);

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

        assertJsonAndStatus(postAmendResponse, CaseNote.class, 200, "A1234AB-update-casenote.json");

        // check the case note now correct
        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AB");

        assertJsonAndStatus(response, CaseNote.class, 200, "A1234AB-casenote.json");
    }

    @Test
    public void testCanCreateAmendments_Normal() {
        oauthMockServer.subGetUserDetails(SECURE_CASENOTE_USER);
        elite2MockServer.subAmendCaseNote("A1234AE", "12345");

        final var token = authTokenHelper.getToken(SECURE_CASENOTE_USER);

        // amend the case note
        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}/{caseNoteId}",
                HttpMethod.PUT,
                createHttpEntity(token, "{ \"text\": \"Amended case note\" }"),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AE", "12345");

        assertJsonAndStatus(response, CaseNote.class, 200, "A1234AE-create-casenote.json");
    }

    @Test
    public void testCanFilterCaseNotes() {
        oauthMockServer.subGetUserDetails(SECURE_CASENOTE_USER);
        elite2MockServer.subGetOffender("A1234AC");
        elite2MockServer.subGetCaseNotesForOffender("A1234AC");

        final var token = authTokenHelper.getToken(SECURE_CASENOTE_USER);

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

        assertJsonAndStatus(response, CaseNote.class, 200, "A1234AC-casenote.json");
    }

    @Test
    public void testCanCreateAndUpdateTypes() {
        oauthMockServer.subGetUserDetails(SYSTEM_USER_READ_WRITE);

        final var token = authTokenHelper.getToken(SYSTEM_USER_READ_WRITE);

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

        assertJsonAndStatus(response, CaseNoteType.class, 201, "newCaseNoteType1.json");

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

        assertJsonAndStatus(responseUpdate, CaseNoteType.class, 200, "updateCaseNoteType1.json");

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

        assertJsonAndStatus(responseSubType, CaseNoteType.class, 201, "newCaseNoteSubType1.json");

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

        assertJsonAndStatus(responseUpdateSubType, CaseNoteType.class, 200, "updateCaseNoteSubType1.json");
    }
}
