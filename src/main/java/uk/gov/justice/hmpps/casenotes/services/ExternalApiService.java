package uk.gov.justice.hmpps.casenotes.services;

import com.google.common.base.Joiner;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    Page<NomisCaseNote> getOffenderCaseNotes(final String offenderIdentifier, final CaseNoteFilter filter, final Pageable pageable) {

        final var paramFilter = getParamFilter(filter, pageable);
        final var url = "/api/offenders/{offenderIdentifier}/case-notes/v2?" + paramFilter;

        return elite2ApiWebClient.get().uri(url, offenderIdentifier)
                .retrieve()
                .toEntity( new ParameterizedTypeReference<RestResponsePage<NomisCaseNote>>() {})
                .map(e -> new PageImpl<NomisCaseNote>(e.getBody().getContent(), e.getBody().getPageable(), e.getBody().getTotalElements()))
                .block();
    }

    private String getParamFilter(final CaseNoteFilter filter, final Pageable pageable) {
        final var paramFilterMap = new HashMap<String, String>();

        if (StringUtils.isNotBlank(filter.getType())) {
            paramFilterMap.put("type", filter.getType());
        }

        if (StringUtils.isNotBlank(filter.getSubType())) {
            paramFilterMap.put("subType", filter.getSubType());
        }

        if (filter.getLocationId() != null) {
            paramFilterMap.put("prisonId", filter.getLocationId());
        }

        if (filter.getStartDate() != null) {
            paramFilterMap.put("from", filter.getStartDate().format(DateTimeFormatter.ISO_DATE));
        }

        if (filter.getEndDate() != null) {
            paramFilterMap.put("to", filter.getEndDate().format(DateTimeFormatter.ISO_DATE));
        }

        paramFilterMap.put("size", String.valueOf(pageable.getPageSize()));
        paramFilterMap.put("page", String.valueOf(pageable.getPageNumber()));

        final var params = paramFilterMap.isEmpty() ? null : Joiner.on("&").withKeyValueSeparator("=").join(paramFilterMap);

        final var sortParams = new StringBuilder();
        pageable.getSort().get().forEach(o -> sortParams.append("sort=").append(o.getProperty()).append(",").append(o.getDirection()));

        return params+(sortParams.length() > 0 ? "&"+sortParams : "");
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

