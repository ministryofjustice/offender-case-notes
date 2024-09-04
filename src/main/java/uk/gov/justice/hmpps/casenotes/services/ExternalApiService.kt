package uk.gov.justice.hmpps.casenotes.services

import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.hmpps.casenotes.dto.BookingIdentifier
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote
import uk.gov.justice.hmpps.casenotes.dto.OffenderBooking
import uk.gov.justice.hmpps.casenotes.dto.UserDetails
import uk.gov.justice.hmpps.casenotes.notes.AmendCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest
import java.time.Duration
import java.time.format.DateTimeFormatter

@Service
class ExternalApiService(
  private val elite2ApiWebClient: WebClient,
  private val oauthApiWebClient: WebClient,
  private val elite2ClientCredentialsWebClient: WebClient,
) {

  fun getMergedIdentifiersByBookingId(bookingId: Long): List<BookingIdentifier> =
    elite2ClientCredentialsWebClient.get()
      .uri("/api/bookings/{bookingId}/identifiers?type={type}", bookingId, "MERGED")
      .retrieve()
      .bodyToFlux<BookingIdentifier>()
      .collectList()
      .block()!!

  fun getBooking(bookingId: Long): OffenderBooking =
    elite2ClientCredentialsWebClient.get().uri("/api/bookings/{bookingId}?basicInfo=true", bookingId)
      .retrieve()
      .bodyToMono<OffenderBooking>()
      .retryOnTransientException()
      .block()!!

  fun getUserDetails(currentUsername: String): UserDetails? =
    oauthApiWebClient.get().uri("/api/user/{username}", currentUsername)
      .exchangeToMono {
        if (it.statusCode() == HttpStatus.NOT_FOUND) {
          Mono.empty()
        } else {
          it.bodyToMono<UserDetails>()
        }
      }
      .retryOnTransientException()
      .block()

  fun getOffenderLocation(offenderIdentifier: String): String =
    elite2ApiWebClient.get().uri("/api/bookings/offenderNo/{offenderNo}", offenderIdentifier)
      .retrieve()
      .bodyToMono<OffenderBooking>()
      .retryOnTransientException()
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
      .retryOnTransientException()
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
      val typeSubTypesParams =
        filter.getTypesAndSubTypes().joinToString(separator = "&", prefix = "&") { "typeSubTypes={typeSubTypes}" }
      return "$params$typeSubTypesParams$sortParams"
    }

    return "$params$sortParams"
  }

  fun createCaseNote(offenderIdentifier: String, newCaseNote: CreateCaseNoteRequest): NomisCaseNote =
    elite2ApiWebClient.post().uri("/api/offenders/{offenderNo}/case-notes", offenderIdentifier)
      .bodyValue(newCaseNote)
      .retrieve()
      .bodyToMono<NomisCaseNote>()
      .retryOnTransientException()
      .block()!!

  fun getOffenderCaseNote(offenderIdentifier: String, caseNoteIdentifier: Long): NomisCaseNote =
    elite2ApiWebClient.get()
      .uri("/api/offenders/{offenderNo}/case-notes/{caseNoteIdentifier}", offenderIdentifier, caseNoteIdentifier)
      .retrieve()
      .bodyToMono<NomisCaseNote>()
      .retryOnTransientException()
      .block()!!

  fun amendOffenderCaseNote(
    offenderIdentifier: String,
    caseNoteIdentifier: Long,
    caseNote: AmendCaseNoteRequest,
  ): NomisCaseNote = elite2ApiWebClient.put()
    .uri("/api/offenders/{offenderNo}/case-notes/{caseNoteIdentifier}", offenderIdentifier, caseNoteIdentifier)
    .bodyValue(caseNote)
    .retrieve()
    .bodyToMono<NomisCaseNote>()
    .retryOnTransientException()
    .block()!!

  fun <T> Mono<T>.retryOnTransientException(): Mono<T> =
    retryWhen(
      Retry.backoff(3, Duration.ofMillis(250))
        .filter {
          it is WebClientRequestException || (it is WebClientResponseException && it.statusCode.is5xxServerError)
        }.onRetryExhaustedThrow { _, signal ->
          signal.failure()
        },
    )
}
