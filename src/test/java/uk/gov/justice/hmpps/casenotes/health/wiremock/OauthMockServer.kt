package uk.gov.justice.hmpps.casenotes.health.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.google.gson.GsonBuilder
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class OAuthExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val oAuthApi = OAuthMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    oAuthApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    oAuthApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    oAuthApi.stop()
  }
}

class OAuthMockServer : WireMockServer(WIREMOCK_PORT) {
  private val gson = GsonBuilder().create()

  fun stubGrantToken() {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/auth/oauth/token"))
        .willReturn(
          WireMock.aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                    "token_type": "bearer",
                    "access_token": "ABCDE"
                }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun subGetUserDetails(username: String) {
    stubFor(
      WireMock.get(WireMock.urlPathMatching(String.format("%s/user/%s", API_PREFIX, username)))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              "{\n" +
                "  \"staffId\": 1111,\n" +
                "  \"username\": \"" + username + "\",\n" +
                "  \"userId\": 1111,\n" +
                "  \"active\": true,\n" +
                "  \"name\": \"Mikey Mouse\",\n" +
                "  \"authSource\": \"nomis\",\n" +
                "  \"activeCaseLoadId\": \"LEI\"\n" +
                "}",
            )
            .withStatus(200),
        ),
    )
  }

  companion object {
    private const val WIREMOCK_PORT = 8998
    private const val API_PREFIX = "/auth/api"
  }
}
