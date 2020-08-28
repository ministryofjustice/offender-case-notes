package uk.gov.justice.hmpps.casenotes.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.util.ReferenceSerializationConfigurer
import lombok.AllArgsConstructor
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import springfox.documentation.builders.AuthorizationCodeGrantBuilder
import springfox.documentation.builders.OAuthBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.AuthorizationScope
import springfox.documentation.service.Contact
import springfox.documentation.service.GrantType
import springfox.documentation.service.SecurityReference
import springfox.documentation.service.SecurityScheme
import springfox.documentation.service.TokenEndpoint
import springfox.documentation.service.TokenRequestEndpoint
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.contexts.SecurityContext
import springfox.documentation.spring.web.json.JacksonModuleRegistrar
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@Configuration
@EnableSwagger2
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
                  "/swagger-ui.html", "/swagger-resources", "/swagger-resources/configuration/ui",
                  "/swagger-resources/configuration/security")
              .permitAll()
              .anyRequest()
              .authenticated()
        }
        .oauth2ResourceServer().jwt().authenticationManager(tokenVerifyingAuthManager)
  }

  @Bean
  fun api(): Docket {
    val docket = Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.basePackage("uk.gov.justice.hmpps.casenotes.controllers"))
        .paths(PathSelectors.any())
        .build()
        .securitySchemes(listOf(securityScheme()))
        .securityContexts(listOf(securityContext()))
        .apiInfo(apiInfo())
    docket.genericModelSubstitutes(Optional::class.java)
    docket.directModelSubstitute(ZonedDateTime::class.java, Date::class.java)
    docket.directModelSubstitute(LocalDateTime::class.java, Date::class.java)
    return docket
  }

  private fun securityScheme(): SecurityScheme {
    val grantType = AuthorizationCodeGrantBuilder()
        .tokenEndpoint(TokenEndpoint("http://localhost:9090/auth/oauth" + "/token", "oauthtoken"))
        .tokenRequestEndpoint(
            TokenRequestEndpoint("http://localhost:9090/auth/oauth" + "/authorize", "swagger-client", "clientsecret"))
        .build()
    return OAuthBuilder().name("spring_oauth")
        .grantTypes(mutableListOf(grantType) as List<GrantType>?)
        .scopes(listOf(*scopes()))
        .build()
  }

  private fun scopes() = arrayOf(
      AuthorizationScope("read", "for read operations"),
      AuthorizationScope("write", "for write operations")
  )

  private fun securityContext() =
      SecurityContext.builder()
          .securityReferences(listOf(SecurityReference("spring_oauth", scopes())))
          .forPaths(PathSelectors.regex("/.*"))
          .build()

  private val version: String
    get() = if (buildProperties == null) "version not available" else buildProperties.version

  private fun contactInfo() = Contact(
      "HMPPS Digital Studio",
      "",
      "feedback@digital.justice.gov.uk")


  private fun apiInfo() = ApiInfo(
      "HMPPS Offender Case Note Documentation",
      "API for Case note details for offenders.",
      this.version,
      "https://gateway.nomis-api.service.justice.gov.uk/auth/terms",
      contactInfo(),
      "Open Government Licence v3.0", "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/", emptyList())


  @Bean
  fun swaggerJacksonModuleRegistrar() = JacksonModuleRegistrar { mapper: ObjectMapper? -> ReferenceSerializationConfigurer.serializeAsComputedRef(mapper) }

}
