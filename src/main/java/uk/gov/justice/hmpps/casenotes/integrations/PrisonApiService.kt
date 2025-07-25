package uk.gov.justice.hmpps.casenotes.integrations

import com.fasterxml.jackson.annotation.JsonAlias
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
class PrisonApiService(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {
  fun getPrisonSwitch(): List<PrisonDetail> = webClient.get().uri {
    it.path(GET_PRISON_SWITCH).build(SERVICE_CODE)
  }.exchangeToMono {
    when (it.statusCode()) {
      HttpStatus.NOT_FOUND -> Mono.just(emptyList())
      HttpStatus.OK -> it.bodyToMono<List<PrisonDetail>>()
      else -> it.createError()
    }
  }.retryOnTransientException().block()!!

  fun alertCaseNotesFor(prisonCode: String): Boolean = getPrisonSwitch().any {
    it.prisonCode == prisonCode || it.prisonCode == "*ALL*"
  }

  companion object {
    const val SERVICE_CODE = "ALERTS_CASE_NOTES"
    const val GET_PRISON_SWITCH = "/api/agency-switches/{serviceCode}"
  }
}

data class PrisonDetail(@JsonAlias("prisonId") val prisonCode: String, @JsonAlias("prison") val description: String)
