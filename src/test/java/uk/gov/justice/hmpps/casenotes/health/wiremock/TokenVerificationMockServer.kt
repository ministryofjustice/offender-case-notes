package uk.gov.justice.hmpps.casenotes.health.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class TokenVerificationExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val tokenVerificationApi = TokenVerificationMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    tokenVerificationApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    tokenVerificationApi.resetRequests()
    tokenVerificationApi.stubVerifyRequest()
  }

  override fun afterAll(context: ExtensionContext) {
    tokenVerificationApi.stop()
  }
}

class TokenVerificationMockServer : WireMockServer(9100) {

  fun stubVerifyRequest(active: Boolean = true) {
    stubFor(
        WireMock.post(WireMock.urlEqualTo("/token/verify"))
            .willReturn(WireMock.aResponse()
                .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
                .withBody(""" {"active": "$active"} """))
    )
  }
}
