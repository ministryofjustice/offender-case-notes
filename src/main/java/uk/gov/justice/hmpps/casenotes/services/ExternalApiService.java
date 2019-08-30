package uk.gov.justice.hmpps.casenotes.services;

import com.google.common.base.Joiner;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteType;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@AllArgsConstructor
public class ExternalApiService {

    private final RestTemplate elite2ApiRestTemplate;
    private final RestTemplate oauthApiRestTemplate;

    List<CaseNoteType> getCaseNoteTypes() {
        return getCaseNoteTypes("/api/reference-domains/caseNoteTypes");
    }

    List<CaseNoteType> getUserCaseNoteTypes() {
        return getCaseNoteTypes("/api/users/me/caseNoteTypes");
    }

    private List<CaseNoteType> getCaseNoteTypes(final String url) {
        final var response = elite2ApiRestTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<CaseNoteType>>() {
                });

        final var body = response.getBody();

        if (body == null) {
            throw EntityNotFoundException.withMessage("Case Note Types Not Found");
        }

        return body;
    }

    String getUserFullName(final String currentUsername) {
        final var response = oauthApiRestTemplate.exchange("/api/user/{username}", HttpMethod.GET, null, Map.class, currentUsername);
        if (response.getBody() != null) {
            return (String) response.getBody().get("name");
        }
        return currentUsername;
    }

    String getOffenderLocation(final String offenderIdentifier) {
        final var response = elite2ApiRestTemplate.exchange("/api/bookings/offenderNo/{offenderNo}", HttpMethod.GET, null, Map.class, offenderIdentifier);
        final var body = response.getBody();
        if (body == null) {
            throw EntityNotFoundException.withId(offenderIdentifier);
        }

        return (String) response.getBody().get("agencyId");
    }

    Page<NomisCaseNote> getOffenderCaseNotes(final String offenderIdentifier, final CaseNoteFilter filter, final int pageLimit, final int pageNumber, final String sortFields, final Sort.Direction direction) {

        final var headers = new HttpHeaders();
        final var offset = pageLimit * pageNumber;
        Map.of("Page-Limit", String.valueOf(pageLimit),
                "Page-Offset", String.valueOf(offset),
                "Sort-Fields", sortFields,
                "Sort-Order", direction.name())
                .forEach(headers::add);

        final var queryFilter = getQueryFilter(filter);
        final var url = "/api/offenders/{offenderIdentifier}/case-notes" + (queryFilter != null ? "?" + queryFilter : "");

        final var response = elite2ApiRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<List<NomisCaseNote>>() {
                }, offenderIdentifier);

        final var body = response.getBody();
        if (body == null) {
            throw EntityNotFoundException.withId(offenderIdentifier);
        }

        return new PageImpl<>(response.getBody(),
                PageRequest.of(pageNumber, pageLimit),
                getHeader(response)
        );
    }

    private int getHeader(final ResponseEntity response) {
        final var responseHeaders = response.getHeaders();
        final var value = responseHeaders.get("Total-Records");
        return value != null && !value.isEmpty() ? Integer.valueOf(value.get(0)) : 0;
    }

    private String getQueryFilter(final CaseNoteFilter filter) {
        final var queryFilters = new ArrayList<String>();

        if (StringUtils.isNotBlank(filter.getType())) {
            queryFilters.add(String.format("type:in:'%s'", filter.getType()));
        }

        if (StringUtils.isNotBlank(filter.getSubType())) {
            queryFilters.add(String.format("subType:in:'%s'", filter.getSubType()));
        }

        if (filter.getLocationId() != null) {
            queryFilters.add(String.format("agencyId:eq:'%s'", filter.getLocationId()));
        }

        final var queryParamsMap = new HashMap<String, String>();
        if (!queryFilters.isEmpty()) {
            queryParamsMap.put("query", StringUtils.join(queryFilters, ",and:"));
        }

        if (filter.getStartDate() != null) {
            queryParamsMap.put("from", filter.getStartDate().format(DateTimeFormatter.ISO_DATE));
        }

        if (filter.getEndDate() != null) {
            queryParamsMap.put("to", filter.getEndDate().format(DateTimeFormatter.ISO_DATE));
        }

        return queryParamsMap.isEmpty() ? null : Joiner.on("&").withKeyValueSeparator("=").join(queryParamsMap);
    }

    NomisCaseNote createCaseNote(final String offenderIdentifier, final NewCaseNote newCaseNote) {
        final var response = elite2ApiRestTemplate.postForEntity("/api/offenders/{offenderNo}/case-notes", newCaseNote, NomisCaseNote.class, offenderIdentifier);
        return Optional.ofNullable(response.getBody()).orElseThrow(EntityNotFoundException.withId(offenderIdentifier));
    }

    NomisCaseNote getOffenderCaseNote(final String offenderIdentifier, final long caseNoteIdentifier) {
        final var response = elite2ApiRestTemplate.getForEntity("/api/offenders/{offenderNo}/case-notes/{caseNoteIdentifier}", NomisCaseNote.class, offenderIdentifier, caseNoteIdentifier);
        return Optional.ofNullable(response.getBody()).orElseThrow(EntityNotFoundException.withId(offenderIdentifier));
    }

    NomisCaseNote amendOffenderCaseNote(final String offenderIdentifier, final long caseNoteIdentifier, final String caseNote) {
        final var response = elite2ApiRestTemplate.exchange("/api/offenders/{offenderNo}/case-notes/{caseNoteIdentifier}", HttpMethod.PUT,
                new HttpEntity<>(caseNote), NomisCaseNote.class, offenderIdentifier, caseNoteIdentifier);
        return Optional.ofNullable(response.getBody()).orElseThrow(EntityNotFoundException.withId(offenderIdentifier));
    }
}

