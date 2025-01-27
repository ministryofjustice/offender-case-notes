package uk.gov.justice.hmpps.casenotes.health.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.hmpps.casenotes.integrations.UserDetails
import uk.gov.justice.hmpps.casenotes.utils.JsonHelper
import java.util.UUID

class ManageUsersApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val manageUsersApi = ManageUsersApiServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    manageUsersApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    manageUsersApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    manageUsersApi.stop()
  }
}

class ManageUsersApiServer : WireMockServer(WIREMOCK_PORT) {
  private val mapper = JsonHelper.objectMapper

  fun stubGetUserDetails(username: String, nomisUser: Boolean = true): StubMapping =
    stubFor(
      get("/users/$username")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                UserDetails(
                  username = username,
                  active = true,
                  name = "Mikey Mouse",
                  authSource = if (nomisUser) "nomis" else "delius",
                  activeCaseLoadId = "MDI",
                  userId = "1111",
                  uuid = UUID.randomUUID(),
                ),
              ),
            )
            .withStatus(200),
        ),
    )

  fun stubGetUserDetails(userDetails: UserDetails): StubMapping =
    stubFor(
      get("/users/${userDetails.username}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(userDetails))
            .withStatus(200),
        ),
    )

  companion object {
    private const val WIREMOCK_PORT = 8100
  }
}
