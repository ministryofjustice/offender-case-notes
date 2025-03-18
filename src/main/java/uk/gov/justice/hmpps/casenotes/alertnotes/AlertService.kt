package uk.gov.justice.hmpps.casenotes.alertnotes

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.hmpps.casenotes.integrations.retryOnTransientException
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.UUID

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

  fun getAlert(uuid: UUID): Alert = webClient.get()
    .uri("/alerts/$uuid")
    .exchangeToMono { res ->
      when (res.statusCode()) {
        HttpStatus.NOT_FOUND -> Mono.empty()
        HttpStatus.OK -> res.bodyToMono<Alert>()
        else -> res.createError()
      }
    }
    .retryOnTransientException()
    .block()!!
}
