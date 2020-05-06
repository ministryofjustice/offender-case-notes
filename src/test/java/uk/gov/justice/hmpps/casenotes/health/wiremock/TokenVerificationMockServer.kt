package uk.gov.justice.hmpps.casenotes.health.wiremock

import com.github.tomakehurst.wiremock.junit.WireMockRule

class TokenVerificationMockServer : WireMockRule(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 9100
  }
}
