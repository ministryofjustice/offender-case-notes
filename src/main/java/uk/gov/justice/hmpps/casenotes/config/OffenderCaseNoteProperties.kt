package uk.gov.justice.hmpps.casenotes.config

import org.hibernate.validator.constraints.URL
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Configuration
@Validated
data class OffenderCaseNoteProperties(
    /** Elite2 API Base URL endpoint ("http://localhost:8080") */
    @Value("\${elite2.api.base.url}") val elite2ApiBaseUrl: @URL String,
    /**  OAUTH2 API Rest URL endpoint ("http://localhost:9090/auth/api") */
    @Value("\${oauth.api.base.url}") val oauthApiBaseUrl: @URL String,
    /** OAUTH2 API Rest URL endpoint ("http://localhost:8100") */
    @Value("\${tokenverification.api.base.url}") val tokenVerificationApiBaseUrl: @URL String,
    @Value("\${api.health-timeout:1s}") val healthTimeout: Duration)
