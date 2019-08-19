package uk.gov.justice.hmpps.casenotes.controllers;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import uk.gov.justice.hmpps.casenotes.dto.CaseNote;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteType;
import uk.gov.justice.hmpps.casenotes.utils.AuthTokenHelper;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forType;
import static uk.gov.justice.hmpps.casenotes.utils.AuthTokenHelper.AuthToken.API_TEST_USER;
import static uk.gov.justice.hmpps.casenotes.utils.AuthTokenHelper.AuthToken.SECURE_CASENOTE_USER;

public class CaseNoteResourceTest extends ResourceTest {

    private static final String CREATE_CASE_NOTE  = "{\"locationId\": \"%s\", \"type\": \"POM\", \"subType\": \"GEN\", \"text\": \"%s\"}";
    private static final String CREATE_CASE_NOTE_WITHOUT_LOC  = "{\"type\": \"POM\", \"subType\": \"GEN\", \"text\": \"%s\"}";

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

        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        assertThat(new JsonContent<CaseNoteType>(getClass(), forType(CaseNoteType.class), response.getBody())).isEqualToJson("caseNoteTypes.json");

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

        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        assertThat(new JsonContent<CaseNoteType>(getClass(), forType(CaseNoteType.class), response.getBody())).isEqualToJson("caseNoteTypesSecure.json");

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

        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        assertThat(new JsonContent<CaseNoteType>(getClass(), forType(CaseNoteType.class), response.getBody())).isEqualToJson("userCaseNoteTypes.json");

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

        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        assertThat(new JsonContent<CaseNoteType>(getClass(), forType(CaseNoteType.class), response.getBody())).isEqualToJson("userCaseNoteTypesSecure.json");

    }

    @Test
    public void testCanCreateAndRetrieveCaseNotesForOffenderSenstive() {
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

        assertThat(postResponse.getStatusCodeValue()).isEqualTo(201);
        assertThat(new JsonContent<CaseNote>(getClass(), forType(CaseNote.class), postResponse.getBody())).isEqualToJson("A1234AA-create-casenote.json");

        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AA");

        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        assertThat(new JsonContent<CaseNote>(getClass(), forType(CaseNote.class), response.getBody())).isEqualToJson("A1234AA-casenote.json");

    }

    @Test
    public void testCanCreateAndRetrieveCaseNotesForOffenderNormal() {
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

        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        assertThat(new JsonContent<CaseNote>(getClass(), forType(CaseNote.class), response.getBody())).isEqualToJson("A1234AA-normal-casenote.json");

    }

    @Test
    public void testCanCreateAmendments() {
        oauthMockServer.subGetUserDetails(SECURE_CASENOTE_USER);
        elite2MockServer.subGetOffender("A1234AB");
        elite2MockServer.subGetCaseNotesForOffender("A1234AB");

        final var token = authTokenHelper.getToken(SECURE_CASENOTE_USER);

        final var postResponse = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.POST,
                createHttpEntity(token, format(CREATE_CASE_NOTE_WITHOUT_LOC, "This is another case note")),
                new ParameterizedTypeReference<CaseNote>() {
                },
                "A1234AB");

        assertThat(postResponse.getStatusCodeValue()).isEqualTo(201);

        final var postAmendResponse = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}/{caseNoteId}",
                HttpMethod.PUT,
                createHttpEntity(token, "Amended case note"),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AB", postResponse.getBody().getCaseNoteId());

        assertThat(postAmendResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(new JsonContent<CaseNote>(getClass(), forType(CaseNote.class), postAmendResponse.getBody())).isEqualToJson("A1234AB-update-casenote.json");

        final var response = testRestTemplate.exchange(
                "/case-notes/{offenderIdentifier}",
                HttpMethod.GET,
                createHttpEntity(token, null),
                new ParameterizedTypeReference<String>() {
                },
                "A1234AB");

        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        assertThat(new JsonContent<CaseNote>(getClass(), forType(CaseNote.class), response.getBody())).isEqualToJson("A1234AB-casenote.json");

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

        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        assertThat(new JsonContent<CaseNote>(getClass(), forType(CaseNote.class), response.getBody())).isEqualToJson("A1234AC-casenote.json");

    }

}
