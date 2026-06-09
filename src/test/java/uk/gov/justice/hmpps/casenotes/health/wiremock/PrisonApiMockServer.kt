package uk.gov.justice.hmpps.casenotes.health.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.hmpps.casenotes.integrations.PrisonDetail
import uk.gov.justice.hmpps.casenotes.utils.JsonHelper.jsonMapper

class PrisonApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonApi.stop()
  }
}

class PrisonApiMockServer : WireMockServer(WIREMOCK_PORT) {
  fun stubPrisonSwitch(serviceCode: String = "ALERTS_CASE_NOTES", response: List<PrisonDetail>) {
    stubFor(
      get(urlPathMatching("/api/agency-switches/$serviceCode"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(response))
            .withStatus(200),
        ),
    )
  }

  fun stubPrisonSwitchNotFound(serviceCode: String = "ALERTS_CASE_NOTES") {
    stubFor(
      get(urlPathMatching("/api/agency-switches/$serviceCode"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404),
        ),
    )
  }

  companion object {
    private const val WIREMOCK_PORT = 8999
  }
}
