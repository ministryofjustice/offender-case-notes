package uk.gov.justice.hmpps.casenotes.services;

import com.google.common.base.Joiner;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.hmpps.casenotes.dto.BookingIdentifier;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteTypeDto;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.OffenderBooking;
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNote;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class ExternalApiService {

    private final WebClient elite2ApiWebClient;
    private final WebClient oauthApiWebClient;
    private final WebClient elite2ClientCredentialsWebClient;

    List<CaseNoteTypeDto> getCaseNoteTypes() {
        return getCaseNoteTypes("/api/reference-domains/caseNoteTypes");
    }

    List<CaseNoteTypeDto> getUserCaseNoteTypes() {
        return getCaseNoteTypes("/api/users/me/caseNoteTypes");
    }

    private List<CaseNoteTypeDto> getCaseNoteTypes(final String url) {
        return elite2ApiWebClient.get().uri(url)
                .retrieve()
                .bodyToMono(
                        new ParameterizedTypeReference<List<CaseNoteTypeDto>>() {
                        })
                .block();
    }

    List<BookingIdentifier> getMergedIdentifiersByBookingId(final Long bookingId) {
        return elite2ClientCredentialsWebClient.get().uri("/api/bookings/{bookingId}/identifiers?type={type}", bookingId, "MERGED")
                .retrieve()
                .bodyToMono(
                        new ParameterizedTypeReference<List<BookingIdentifier>>() {
                        })
                .block();
    }

    OffenderBooking getBooking(final Long bookingId) {
        return elite2ClientCredentialsWebClient.get().uri("/api/bookings/{bookingId}?basicInfo=true", bookingId)
                .retrieve()
                .bodyToMono(OffenderBooking.class)
                .block();
    }

    String getUserFullName(final String currentUsername) {
        return oauthApiWebClient.get().uri("/api/user/{username}", currentUsername)
                .retrieve()
                .bodyToMono(
                        new ParameterizedTypeReference<Map<String, String>>() {
                        })
                .blockOptional().map(u -> u.getOrDefault("name", currentUsername)).orElse(currentUsername);
    }

    String getOffenderLocation(final String offenderIdentifier) {
        return elite2ApiWebClient.get().uri("/api/bookings/offenderNo/{offenderNo}", offenderIdentifier)
                .retrieve()
                .bodyToMono(OffenderBooking.class)
                .map(OffenderBooking::getAgencyId)
                .block();
    }

    Page<NomisCaseNote> getOffenderCaseNotes(final String offenderIdentifier, final CaseNoteFilter filter, final int pageLimit, final int pageNumber, final String sortFields, final Sort.Direction direction) {

        final var offset = pageLimit * pageNumber;
        final var headerMap = Map.of("Page-Limit", String.valueOf(pageLimit),
                "Page-Offset", String.valueOf(offset),
                "Sort-Fields", sortFields,
                "Sort-Order", direction.name());

        final var queryFilter = getQueryFilter(filter);
        final var url = "/api/offenders/{offenderIdentifier}/case-notes" + (queryFilter != null ? "?" + queryFilter : "");

        return elite2ApiWebClient.get().uri(url, offenderIdentifier)
                .headers(
                        c -> {
                            c.add("Page-Limit", String.valueOf(pageLimit));
                            c.add("Page-Offset", String.valueOf(offset));
                            c.add("Sort-Fields", sortFields);
                            c.add("Sort-Order", direction.name());
                        })
                .retrieve()
                .toEntityList(NomisCaseNote.class)
                .map(e -> new PageImpl<>(e.getBody(), PageRequest.of(pageNumber, pageLimit), getHeader(e.getHeaders())))
                .block();
    }

    private int getHeader(final HttpHeaders responseHeaders) {
        final var value = responseHeaders.getOrDefault("Total-Records", Collections.emptyList());
        return !value.isEmpty() ? Integer.parseInt(value.get(0)) : 0;
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
        return elite2ApiWebClient.post().uri("/api/offenders/{offenderNo}/case-notes", offenderIdentifier)
                .bodyValue(newCaseNote)
                .retrieve()
                .bodyToMono(NomisCaseNote.class)
                .block();
    }

    NomisCaseNote getOffenderCaseNote(final String offenderIdentifier, final long caseNoteIdentifier) {
        return elite2ApiWebClient.get().uri("/api/offenders/{offenderNo}/case-notes/{caseNoteIdentifier}", offenderIdentifier, caseNoteIdentifier)
                .retrieve()
                .bodyToMono(NomisCaseNote.class)
                .block();
    }

    NomisCaseNote amendOffenderCaseNote(final String offenderIdentifier, final long caseNoteIdentifier, final UpdateCaseNote caseNote) {
        return elite2ApiWebClient.put().uri("/api/offenders/{offenderNo}/case-notes/{caseNoteIdentifier}", offenderIdentifier, caseNoteIdentifier)
                .bodyValue(caseNote)
                .retrieve()
                .bodyToMono(NomisCaseNote.class)
                .block();
    }
}

