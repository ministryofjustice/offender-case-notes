package uk.gov.justice.hmpps.casenotes.health.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
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
  }

  override fun afterAll(context: ExtensionContext) {
    tokenVerificationApi.stop()
  }
}

class TokenVerificationMockServer : WireMockServer(9100)
