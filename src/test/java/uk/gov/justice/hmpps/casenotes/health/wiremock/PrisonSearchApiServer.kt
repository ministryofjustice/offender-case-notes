package uk.gov.justice.hmpps.casenotes.health.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class PrisonerSearchApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonerSearchApi = PrisonSearchApiServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonerSearchApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonerSearchApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonerSearchApi.stop()
  }
}

class PrisonSearchApiServer : WireMockServer(WIREMOCK_PORT) {
  fun stubPrisonerDetails(personIdentifier: String, prisonId: String = "MDI") {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/prisoner/$personIdentifier"))
        .willReturn(
          WireMock.aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
              {
                "prisonerNumber": "$personIdentifier",
                "prisonId": "$prisonId"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  companion object {
    private const val WIREMOCK_PORT = 8200
  }
}
