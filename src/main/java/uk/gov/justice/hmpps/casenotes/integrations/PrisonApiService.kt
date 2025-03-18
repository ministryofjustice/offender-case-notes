package uk.gov.justice.hmpps.casenotes.integrations

import com.fasterxml.jackson.annotation.JsonAlias
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class PrisonApiService(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {
  fun getPrisonSwitch(): List<PrisonDetail> = webClient.get().uri {
    it.path(GET_PRISON_SWITCH).build(SERVICE_CODE)
  }.retrieve()
    .bodyToMono<List<PrisonDetail>>()
    .retryOnTransientException()
    .block()!!

  fun alertCaseNotesFor(prisonCode: String): Boolean = getPrisonSwitch().any {
    it.prisonCode == prisonCode || it.prisonCode == "*ALL*"
  }

  companion object {
    const val SERVICE_CODE = "ALERTS_CASE_NOTES"
    const val GET_PRISON_SWITCH = "/api/service-prisons/{serviceCode}"
  }
}

data class PrisonDetail(@JsonAlias("prisonId") val prisonCode: String, @JsonAlias("prison") val description: String)
