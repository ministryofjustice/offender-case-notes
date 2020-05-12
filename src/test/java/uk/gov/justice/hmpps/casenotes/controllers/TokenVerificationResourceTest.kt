package uk.gov.justice.hmpps.casenotes.controllers

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension.Companion.elite2Api
import uk.gov.justice.hmpps.casenotes.health.wiremock.TokenVerificationExtension.Companion.tokenVerificationApi
import wiremock.org.eclipse.jetty.http.HttpHeader

class TokenVerificationResourceTest : ResourceTest() {
  @Test
  fun `test jwt gets passed through to token verification`() {
    val jwt = createJwt("API_TEST_USER")

    elite2Api.subGetCaseNoteTypes()
    webTestClient.get().uri("/case-notes/types")
        .headers(addBearerToken(jwt))
        .exchange()
        .expectStatus().isOk

    tokenVerificationApi.verify(postRequestedFor(urlEqualTo("/token/verify"))
        .withHeader(HttpHeader.AUTHORIZATION.asString(), equalTo("Bearer $jwt")))
  }

  @Test
  fun `jwt token failure causes 401 failure to client`() {
    val jwt = createJwt("API_TEST_USER")

    tokenVerificationApi.stubVerifyRequest(false)

    elite2Api.subGetCaseNoteTypes()
    webTestClient.get().uri("/case-notes/types")
        .headers(addBearerToken(jwt))
        .exchange()
        .expectStatus().isUnauthorized

    tokenVerificationApi.verify(postRequestedFor(urlEqualTo("/token/verify"))
        .withHeader(HttpHeader.AUTHORIZATION.asString(), equalTo("Bearer $jwt")))
  }
}
