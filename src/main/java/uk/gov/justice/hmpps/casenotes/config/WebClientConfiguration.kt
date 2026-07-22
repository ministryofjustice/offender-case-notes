package uk.gov.justice.hmpps.casenotes.config

import org.hibernate.validator.constraints.URL
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value($$"${prison.api.base.url}") private val prisonApiBaseUrl: @URL String,
  @param:Value($$"${oauth.api.base.url}") private val oauthApiBaseUrl: @URL String,
  @param:Value($$"${prisoner-search.api.base.url}") private val prisonerSearchApiBaseUrl: @URL String,
  @param:Value($$"${manage-users.api.base.url}") private val manageUsersApiBaseUrl: @URL String,
  @param:Value($$"${alerts.api.base.url}") private val alertsApiBaseUrl: @URL String,
  @param:Value($$"${tokenverification.api.base.url}") private val tokenVerificationApiBaseUrl: @URL String,
  @param:Value($$"${api.health-timeout:1s}") private val healthTimeout: Duration,
  @param:Value($$"${api.response-timeout:2s}") private val responseTimeout: Duration,
) {
  @Bean
  fun prisonApiHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(prisonApiBaseUrl, healthTimeout)

  @Bean
  fun oauthApiHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(oauthApiBaseUrl, healthTimeout)

  @Bean
  fun prisonerSearchApiHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(prisonerSearchApiBaseUrl, healthTimeout)

  @Bean
  fun manageUsersApiHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(manageUsersApiBaseUrl, healthTimeout)

  @Bean
  fun tokenVerificationApiHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(tokenVerificationApiBaseUrl, healthTimeout)

  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, "default", prisonApiBaseUrl, responseTimeout)

  @Bean
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, "default", prisonerSearchApiBaseUrl, responseTimeout)

  @Bean
  fun manageUsersWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, "default", manageUsersApiBaseUrl, responseTimeout)

  @Bean
  fun alertsWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, "default", alertsApiBaseUrl, responseTimeout)

  @Bean
  fun tokenVerificationApiWebClient(builder: Builder): WebClient = builder.baseUrl(tokenVerificationApiBaseUrl).build()
}
