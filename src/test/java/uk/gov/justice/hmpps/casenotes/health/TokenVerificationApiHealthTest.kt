package uk.gov.justice.hmpps.casenotes.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.web.reactive.function.client.WebClient

class TokenVerificationApiHealthTest {
  val webClient: WebClient = mock()

  @Test
  fun `token verification disabled health passes`() {
    val health = TokenVerificationApiHealth(webClient, false).health()
    assertThat(health.toString()).isEqualTo("UP {TokenVerification=Disabled}")
    verifyNoInteractions(webClient)
  }
}
