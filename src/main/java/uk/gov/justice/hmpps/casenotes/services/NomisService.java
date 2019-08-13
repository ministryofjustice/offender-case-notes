package uk.gov.justice.hmpps.casenotes.services;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteType;

import java.util.List;

@Service
public class NomisService {

    private final RestTemplate elite2ApiRestTemplate;

    public NomisService(RestTemplate elite2ApiRestTemplate) {
        this.elite2ApiRestTemplate = elite2ApiRestTemplate;
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

    public String getUserDetails(String currentUsername) {
        return currentUsername;
    }
}

