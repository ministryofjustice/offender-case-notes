package uk.gov.justice.hmpps.casenotes.services;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteType;

import java.util.List;
import java.util.Map;

@Service
public class ExternalApiService {

    private final RestTemplate elite2ApiRestTemplate;

    private final RestTemplate oauthApiRestTemplate;

    public ExternalApiService(RestTemplate elite2ApiRestTemplate, RestTemplate oauthApiRestTemplate) {
        this.elite2ApiRestTemplate = elite2ApiRestTemplate;
        this.oauthApiRestTemplate = oauthApiRestTemplate;
    }

    public List<CaseNoteType> getCaseNoteTypes() {
        return getCaseNoteTypes("/reference-domains/caseNoteTypes");
    }

    public List<CaseNoteType> getUserCaseNoteTypes() {
        return getCaseNoteTypes("/users/me/caseNoteTypes");
    }

    private List<CaseNoteType> getCaseNoteTypes(String url) {
        final var response = elite2ApiRestTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<CaseNoteType>>() {
                });

        final var body = response.getBody();

        if (body == null) {
            throw EntityNotFoundException.withMessage("Case Note Types Not Found");
        }

        return body;
    }

    public String getUserFullName(final String currentUsername) {
        final var response = oauthApiRestTemplate.exchange("/user/{username}", HttpMethod.GET, null, Map.class, currentUsername);
        if (response.getBody() != null) {
            return (String)response.getBody().get("name");
        }
        return currentUsername;
    }

    public String getOffenderLocation(final String offenderIdentifier) {
        final var response = elite2ApiRestTemplate.exchange("/bookings/offenderNo/{offenderNo}", HttpMethod.GET, null, Map.class, offenderIdentifier);
        final var body = response.getBody();
        if (body == null) {
            throw EntityNotFoundException.withId(offenderIdentifier);
        }

        return (String)response.getBody().get("agencyId");
    }

}

