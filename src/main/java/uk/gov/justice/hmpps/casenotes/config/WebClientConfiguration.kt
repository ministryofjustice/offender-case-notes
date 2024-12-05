package uk.gov.justice.hmpps.casenotes.config

import io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS
import io.netty.channel.ChannelOption.SO_KEEPALIVE
import io.netty.channel.epoll.EpollChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.hibernate.validator.constraints.URL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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
import reactor.netty.http.client.HttpClient.create
import uk.gov.justice.hmpps.casenotes.legacy.utils.UserContext
import java.time.Duration
import java.time.Duration.ofSeconds

@Configuration
class WebClientConfiguration(
  @Value("\${elite2.api.base.url}") private val elite2ApiBaseUrl: @URL String,
  @Value("\${oauth.api.base.url}") private val oauthApiBaseUrl: @URL String,
  @Value("\${prisoner-search.api.base.url}") private val prisonerSearchApiBaseUrl: @URL String,
  @Value("\${manage-users.api.base.url}") private val manageUsersApiBaseUrl: @URL String,
  @Value("\${tokenverification.api.base.url}") private val tokenVerificationApiBaseUrl: @URL String,
  @Value("\${api.health-timeout:1s}") private val healthTimeout: Duration,
  @Value("\${api.response-timeout:2s}") private val responseTimeout: Duration,
) {

  @Bean
  fun elite2ApiWebClient(builder: Builder): WebClient = createForwardAuthWebClient(builder, elite2ApiBaseUrl)

  @Bean
  fun elite2ApiHealthWebClient(builder: Builder): WebClient = createHealthClient(builder, elite2ApiBaseUrl)

  @Bean
  fun oauthApiWebClient(builder: Builder): WebClient = createForwardAuthWebClient(builder, oauthApiBaseUrl)

  @Bean
  fun oauthApiHealthWebClient(builder: Builder): WebClient = createHealthClient(builder, oauthApiBaseUrl)

  @Bean
  fun tokenVerificationApiWebClient(builder: Builder): WebClient = builder.baseUrl(tokenVerificationApiBaseUrl)
    .clientConnector(ReactorClientHttpConnector(create().warmupWithHealthPing(tokenVerificationApiBaseUrl))).build()

  @Bean
  fun tokenVerificationApiHealthWebClient(builder: Builder): WebClient =
    createHealthClient(builder, tokenVerificationApiBaseUrl)

  private fun createForwardAuthWebClient(builder: Builder, url: @URL String): WebClient = builder.baseUrl(url)
    .filter(addAuthHeaderFilterFunction())
    .clientConnector(clientConnector { it.warmupWithHealthPing(tokenVerificationApiBaseUrl) }).build()

  private fun createHealthClient(builder: Builder, url: @URL String): WebClient {
    val httpClient = create()
      .warmupWithHealthPing(url)
      .option(CONNECT_TIMEOUT_MILLIS, healthTimeout.toMillis().toInt())
      .doOnConnected { connection ->
        connection.addHandlerLast(ReadTimeoutHandler(healthTimeout.toSeconds().toInt()))
          .addHandlerLast(WriteTimeoutHandler(healthTimeout.toSeconds().toInt()))
      }
    return builder.clientConnector(ReactorClientHttpConnector(httpClient)).baseUrl(url).build()
  }

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction =
    ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
      val filtered = ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, UserContext.getAuthToken())
        .build()
      next.exchange(filtered)
    }

  @Bean
  fun authorizedClientManagerAppScope(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?,
  ): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService,
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  @Bean
  fun elite2ClientCredentialsWebClient(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: Builder,
  ): WebClient = getOAuthWebClient(authorizedClientManager, builder, elite2ApiBaseUrl)

  @Bean
  fun prisonerSearchWebClient(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: Builder,
  ): WebClient = getOAuthWebClient(authorizedClientManager, builder, prisonerSearchApiBaseUrl)

  @Bean
  fun manageUsersWebClient(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: Builder,
  ): WebClient = getOAuthWebClient(authorizedClientManager, builder, manageUsersApiBaseUrl)

  private fun getOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: Builder,
    rootUri: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("default")
    return builder.baseUrl(rootUri)
      .clientConnector(clientConnector())
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  private fun clientConnector(consumer: ((HttpClient) -> Unit)? = null): ReactorClientHttpConnector {
    val client = create().responseTimeout(ofSeconds(responseTimeout.toSeconds()))
      .option(CONNECT_TIMEOUT_MILLIS, 1000)
      .option(SO_KEEPALIVE, true)
      // this will show a warning on apple (arm) architecture but will work on linux x86 container
      .option(EpollChannelOption.TCP_KEEPINTVL, 60)
    consumer?.also { it.invoke(client) }
    return ReactorClientHttpConnector(client)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private fun HttpClient.warmupWithHealthPing(baseUrl: String): HttpClient {
    log.info("Warming up web client for {}", baseUrl)
    warmup().block()
    log.info("Warming up web client for {} halfway through, now calling health ping", baseUrl)
    try {
      baseUrl("$baseUrl/health/ping").get().response().block(ofSeconds(30))
    } catch (e: RuntimeException) {
      log.error("Caught exception during warm up, carrying on regardless", e)
    }
    log.info("Warming up web client completed for {}", baseUrl)
    return this
  }
}
