package uk.gov.justice.hmpps.casenotes.alertbackfill

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.hmpps.casenotes.integrations.retryOnTransientException
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE

@Service
class AlertService(@Qualifier("alertsWebClient") private val webClient: WebClient) {
  fun getAlertsOfInterest(personIdentifier: String, from: LocalDate, to: LocalDate): CaseNoteAlertResponse = webClient.get()
    .uri {
      it.path("/alerts/case-notes/{personIdentifier}")
      it.queryParam("from", ISO_DATE.format(from))
      it.queryParam("to", ISO_DATE.format(to))
      it.build(personIdentifier)
    }
    .exchangeToMono { res ->
      when (res.statusCode()) {
        HttpStatus.NOT_FOUND -> Mono.empty()
        HttpStatus.OK -> res.bodyToMono<CaseNoteAlertResponse>()
        else -> res.createError()
      }
    }
    .retryOnTransientException()
    .block()!!
}
