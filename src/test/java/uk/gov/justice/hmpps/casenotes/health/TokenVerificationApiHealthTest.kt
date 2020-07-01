package uk.gov.justice.hmpps.casenotes.health

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class TokenVerificationApiHealthTest {
  val webClient: WebClient = mock()

  @Test
  fun `token verification disabled health passes`() {
    val health = TokenVerificationApiHealth(webClient, false).health()
    assertThat(health.toString()).isEqualTo("UP {TokenVerification=Disabled}")
    verifyZeroInteractions(webClient)
  }
}
