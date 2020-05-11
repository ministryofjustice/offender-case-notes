package uk.gov.justice.hmpps.casenotes.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.hibernate.validator.constraints.URL
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient
import uk.gov.justice.hmpps.casenotes.utils.UserContext

@Configuration
class WebClientConfiguration(private val prop: OffenderCaseNoteProperties) {
  @Bean
  fun elite2ApiWebClient(builder: Builder): WebClient = createForwardAuthWebClient(builder, prop.elite2ApiBaseUrl)

  @Bean
  fun elite2ApiHealthWebClient(builder: Builder): WebClient = createHealthClient(builder, prop.elite2ApiBaseUrl)

  @Bean
  fun oauthApiWebClient(builder: Builder): WebClient = createForwardAuthWebClient(builder, prop.oauthApiBaseUrl)

  @Bean
  fun oauthApiHealthWebClient(builder: Builder): WebClient = createHealthClient(builder, prop.oauthApiBaseUrl)

  @Bean
  fun tokenVerificationApiWebClient(builder: Builder): WebClient = createForwardAuthWebClient(builder, prop.tokenVerificationApiBaseUrl)

  @Bean
  fun tokenVerificationApiHealthWebClient(builder: Builder): WebClient = createHealthClient(builder, prop.tokenVerificationApiBaseUrl)

  private fun createForwardAuthWebClient(builder: Builder, url: @URL String) =
      builder.baseUrl(url)
          .filter(addAuthHeaderFilterFunction())
          .build()

  private fun createHealthClient(builder: Builder, url: @URL String): WebClient {
    val timeout = prop.healthTimeout
    val tcpClient = TcpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout.toMillis().toInt())
        .doOnConnected { connection ->
          connection.addHandlerLast(ReadTimeoutHandler(timeout.toSeconds().toInt()))
              .addHandlerLast(WriteTimeoutHandler(timeout.toSeconds().toInt()))
        }
    return builder.clientConnector(ReactorClientHttpConnector(HttpClient.from(tcpClient)))
        .baseUrl(url).build()
  }

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction =
      ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
        val filtered = ClientRequest.from(request)
            .header(HttpHeaders.AUTHORIZATION, UserContext.getAuthToken())
            .build()
        next.exchange(filtered)
      }

  @Bean
  fun authorizedClientManagerAppScope(clientRegistrationRepository: ClientRegistrationRepository?,
                                      oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  @Bean
  fun elite2ClientCredentialsWebClient(
      @Qualifier(value = "authorizedClientManagerAppScope")
      authorizedClientManager: OAuth2AuthorizedClientManager,
      builder: Builder): WebClient =
      getOAuthWebClient(authorizedClientManager, builder, prop.elite2ApiBaseUrl)

  private fun getOAuthWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder, rootUri: String): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("elite2-api")
    return builder.baseUrl(rootUri)
        .apply(oauth2Client.oauth2Configuration())
        .build()
  }
}
