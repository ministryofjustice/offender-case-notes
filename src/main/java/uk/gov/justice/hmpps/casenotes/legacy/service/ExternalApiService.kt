package uk.gov.justice.hmpps.casenotes.legacy.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.hmpps.casenotes.integrations.retryOnTransientException
import uk.gov.justice.hmpps.casenotes.legacy.dto.NomisCaseNote
import uk.gov.justice.hmpps.casenotes.notes.AmendCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteFilter
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest
import java.time.format.DateTimeFormatter

@Service
class ExternalApiService(@Qualifier("elite2ApiWebClient") private val elite2ApiWebClient: WebClient) {
  fun getOffenderCaseNotes(
    offenderIdentifier: String,
    filter: CaseNoteFilter,
    pageable: Pageable,
  ): Page<NomisCaseNote> {
    val paramFilter = getParamFilter(filter, pageable)
    val url = "/api/offenders/{offenderIdentifier}/case-notes/v2?$paramFilter"
    return elite2ApiWebClient.get()
      .uri(url, offenderIdentifier, *filter.getTypesAndSubTypes().asPrisonApiParams().toTypedArray())
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
      val typeSubTypesParams = filter.getTypesAndSubTypes().asPrisonApiParams()
        .joinToString(separator = "&", prefix = "&") { "typeSubTypes={typeSubTypes}" }
      return "$params$typeSubTypesParams$sortParams"
    }

    return "$params$sortParams"
  }

  fun createCaseNote(offenderIdentifier: String, newCaseNote: CreateCaseNoteRequest): NomisCaseNote = elite2ApiWebClient.post().uri("/api/offenders/{offenderNo}/case-notes", offenderIdentifier)
    .bodyValue(newCaseNote)
    .retrieve()
    .bodyToMono<NomisCaseNote>()
    .retryOnTransientException()
    .block()!!

  fun getOffenderCaseNote(offenderIdentifier: String, caseNoteIdentifier: Long): NomisCaseNote = elite2ApiWebClient.get()
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

  private fun Map<String, Set<String>>.asPrisonApiParams(): List<String> = entries.flatMap { e -> if (e.value.isEmpty()) setOf(e.key) else e.value.map { "${e.key}+$it" } }
}
