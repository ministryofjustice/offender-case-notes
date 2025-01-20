package uk.gov.justice.hmpps.casenotes.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration(private val tokenVerifyingAuthManager: TokenVerifyingAuthManager) {
  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      headers { frameOptions { sameOrigin = true } }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      // Can't have CSRF protection as requires session
      csrf { disable() }
      authorizeHttpRequests {
        listOf(
          "/webjars/**", "/favicon.ico",
          "/health/**", "/info", "/csrf",
          "/v3/api-docs/**", "/api/swagger.json", "/swagger-ui/**",
          "/v3/api-docs", "/swagger-ui.html",
          "/swagger-resources", "/swagger-resources/configuration/ui", "/swagger-resources/configuration/security",
          "/queue-admin/retry-all-dlqs", "/ping",
          "/resend-person-case-note-events",
        ).forEach { authorize(it, permitAll) }
        authorize(anyRequest, authenticated)
      }
      oauth2ResourceServer { jwt { authenticationManager = tokenVerifyingAuthManager } }
    }
    return http.build()
  }
}
