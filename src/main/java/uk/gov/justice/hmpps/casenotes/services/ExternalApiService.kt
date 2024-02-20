package uk.gov.justice.hmpps.casenotes.services

import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.casenotes.dto.BookingIdentifier
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteTypeDto
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote
import uk.gov.justice.hmpps.casenotes.dto.OffenderBooking
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNote
import java.time.format.DateTimeFormatter

@Service
class ExternalApiService(
  private val elite2ApiWebClient: WebClient,
  private val oauthApiWebClient: WebClient,
  private val elite2ClientCredentialsWebClient: WebClient,
) {

  fun getCaseNoteTypes(): List<CaseNoteTypeDto> = getCaseNoteTypes("/api/reference-domains/caseNoteTypes")
  fun getUserCaseNoteTypes(): List<CaseNoteTypeDto> = getCaseNoteTypes("/api/users/me/caseNoteTypes")

  private fun getCaseNoteTypes(url: String): List<CaseNoteTypeDto> =
    elite2ApiWebClient.get().uri(url)
      .retrieve()
      .bodyToMono(
        object : ParameterizedTypeReference<List<CaseNoteTypeDto>>() {},
      )
      .block()!!

  fun getMergedIdentifiersByBookingId(bookingId: Long): List<BookingIdentifier> =
    elite2ClientCredentialsWebClient.get()
      .uri("/api/bookings/{bookingId}/identifiers?type={type}", bookingId, "MERGED")
      .retrieve()
      .bodyToMono(
        object : ParameterizedTypeReference<List<BookingIdentifier>>() {},
      )
      .block()!!

  fun getBooking(bookingId: Long): OffenderBooking =
    elite2ClientCredentialsWebClient.get().uri("/api/bookings/{bookingId}?basicInfo=true", bookingId)
      .retrieve()
      .bodyToMono(OffenderBooking::class.java)
      .block()!!

  fun getUserFullName(currentUsername: String): String =
    oauthApiWebClient.get().uri("/api/user/{username}", currentUsername)
      .retrieve()
      .bodyToMono(
        object : ParameterizedTypeReference<Map<String, String>>() {},
      )
      .blockOptional().map { u -> u["name"] ?: currentUsername }
      .orElse(currentUsername)

  fun getOffenderLocation(offenderIdentifier: String): String =
    elite2ApiWebClient.get().uri("/api/bookings/offenderNo/{offenderNo}", offenderIdentifier)
      .retrieve()
      .bodyToMono(OffenderBooking::class.java)
      .map { it.agencyId }
      .block()!!

  fun getOffenderCaseNotes(
    offenderIdentifier: String,
    filter: CaseNoteFilter,
    pageable: Pageable,
  ): Page<NomisCaseNote> {
    val paramFilter = getParamFilter(filter, pageable)
    val url = "/api/offenders/{offenderIdentifier}/case-notes/v2?$paramFilter"
    return elite2ApiWebClient.get().uri(url, offenderIdentifier, *filter.getTypesAndSubTypes().toTypedArray())
      .retrieve()
      .toEntity(object : ParameterizedTypeReference<RestResponsePage<NomisCaseNote>>() {})
      .map { e: ResponseEntity<RestResponsePage<NomisCaseNote>> ->
        PageImpl(
          e.body!!.content,
          e.body!!.pageable,
          e.body!!.totalElements,
        )
      }
      .block()!!
  }

  private fun getParamFilter(filter: CaseNoteFilter, pageable: Pageable): String {
    val paramFilterMap = mutableMapOf(
      "page" to pageable.pageNumber.toString(),
      "size" to pageable.pageSize.toString(),
    ).apply {
      filter.locationId?.let { this["prisonId"] = filter.locationId }
      filter.startDate?.let { this["from"] = filter.startDate.format(DateTimeFormatter.ISO_DATE) }
      filter.endDate?.let { this["to"] = filter.endDate.format(DateTimeFormatter.ISO_DATE) }
    }
    val params = paramFilterMap.entries.joinToString(separator = "&") { (key, value) -> "$key=$value" }
    val sortParams = pageable.sort.map {
      val mappedProperty = if (it.property == "creationDateTime") "createDatetime" else it.property
      "sort=$mappedProperty,${it.direction}"
    }
      .joinToString(separator = "&", prefix = "&")

    if (filter.getTypesAndSubTypes().isNotEmpty()) {
      val typeSubTypesParams = filter.getTypesAndSubTypes().joinToString(separator = "&", prefix = "&") { _ -> "typeSubTypes={typeSubTypes}" }
      return "$params$typeSubTypesParams$sortParams"
    }

    return "$params$sortParams"
  }

  fun createCaseNote(offenderIdentifier: String, newCaseNote: NewCaseNote): NomisCaseNote =
    elite2ApiWebClient.post().uri("/api/offenders/{offenderNo}/case-notes", offenderIdentifier)
      .bodyValue(newCaseNote)
      .retrieve()
      .bodyToMono(NomisCaseNote::class.java)
      .block()!!

  fun getOffenderCaseNote(offenderIdentifier: String, caseNoteIdentifier: Long): NomisCaseNote =
    elite2ApiWebClient.get()
      .uri("/api/offenders/{offenderNo}/case-notes/{caseNoteIdentifier}", offenderIdentifier, caseNoteIdentifier)
      .retrieve()
      .bodyToMono(NomisCaseNote::class.java)
      .block()!!

  fun amendOffenderCaseNote(
    offenderIdentifier: String,
    caseNoteIdentifier: Long,
    caseNote: UpdateCaseNote,
  ): NomisCaseNote = elite2ApiWebClient.put()
    .uri("/api/offenders/{offenderNo}/case-notes/{caseNoteIdentifier}", offenderIdentifier, caseNoteIdentifier)
    .bodyValue(caseNote)
    .retrieve()
    .bodyToMono(NomisCaseNote::class.java)
    .block()!!
}
