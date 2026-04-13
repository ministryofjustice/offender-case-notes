package uk.gov.justice.hmpps.casenotes.alertnotes

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.hmpps.casenotes.utils.JsonHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE

class AlertsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val alertsApi = AlertsApiServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    alertsApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    alertsApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    alertsApi.stop()
  }
}

class AlertsApiServer : WireMockServer(WIREMOCK_PORT) {
  private val mapper = JsonHelper.jsonMapper

  fun withPersonIdentifiers(from: LocalDate, to: LocalDate, vararg prisonNumbers: String): StubMapping = stubFor(
    get(urlPathMatching("/alerts/case-notes/changed"))
      .withQueryParam("from", equalTo(ISO_DATE.format(from)))
      .withQueryParam("to", equalTo(ISO_DATE.format(to)))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(PersonIdentifiersOfInterest(prisonNumbers.toSet())))
          .withStatus(200),
      ),
  )

  fun withAlerts(personIdentifier: String, from: LocalDate, to: LocalDate, response: CaseNoteAlertResponse): StubMapping = stubFor(
    get(urlPathMatching("/alerts/case-notes/$personIdentifier"))
      .withQueryParam("from", equalTo(ISO_DATE.format(from)))
      .withQueryParam("to", equalTo(ISO_DATE.format(to)))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(response))
          .withStatus(200),
      ),
  )

  fun withAlert(alert: Alert): StubMapping = stubFor(
    get(urlPathMatching("/alerts/${alert.alertUuid}"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(alert))
          .withStatus(200),
      ),
  )

  companion object {
    private const val WIREMOCK_PORT = 9999
  }
}
