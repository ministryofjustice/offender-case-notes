package uk.gov.justice.hmpps.casenotes.integrations

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class PrisonerSearchService(@Qualifier("prisonerSearchWebClient") private val webClient: WebClient) {
  fun getPrisonerDetails(personIdentifier: String): PrisonerDetail = webClient.get().uri {
    it.path(GET_PRISONER_DETAIL).build(personIdentifier)
  }.retrieve()
    .bodyToMono<PrisonerDetail>()
    .retryOnTransientException()
    .block()!!

  companion object {
    const val GET_PRISONER_DETAIL = "/prisoner/{id}"
  }
}

data class PrisonerDetail(val prisonerNumber: String, val prisonId: String)
