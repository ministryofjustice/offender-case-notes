package uk.gov.justice.hmpps.casenotes.config

import lombok.AllArgsConstructor
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
@AllArgsConstructor
class ResourceServerConfiguration(private val tokenVerifyingAuthManager: TokenVerifyingAuthManager) : WebSecurityConfigurerAdapter() {
  private val buildProperties: BuildProperties? = null

  @Throws(Exception::class)
  public override fun configure(http: HttpSecurity) {
    http.headers().frameOptions().sameOrigin().and()
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Can't have CSRF protection as requires session
      .and().csrf().disable()
      .authorizeRequests { auth ->
        auth
          .antMatchers(
            "/webjars/**",
            "/favicon.ico",
            "/csrf",
            "/health/**",
            "/info",
            "/ping",
            "/h2-console/**",
            "/v2/api-docs",
            "/swagger-ui/**", "/swagger-resources", "/swagger-resources/configuration/ui",
            "/swagger-resources/configuration/security",
            "/queue-admin/retry-all-dlqs"
          )
          .permitAll()
          .anyRequest()
          .authenticated()
      }
      .oauth2ResourceServer().jwt().authenticationManager(tokenVerifyingAuthManager)
  }
}
