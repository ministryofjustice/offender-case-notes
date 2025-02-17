package uk.gov.justice.hmpps.casenotes.config

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.expression.BeanFactoryResolver
import org.springframework.expression.spel.SpelEvaluationException
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.method.HandlerMethod

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties, private val context: ApplicationContext) {
  private val version: String = buildProperties.version

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://offender-case-notes.service.justice.gov.uk").description("Prod"),
        Server().url("https://preprod.offender-case-notes.service.justice.gov.uk").description("PreProd"),
        Server().url("https://dev.offender-case-notes.service.justice.gov.uk").description("Development"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .info(
      Info().title("HMPPS Offender Case Notes")
        .version(version)
        .description("HMPPS Offender Case Notes API")
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )

  @Bean
  fun preAuthorizeCustomizer(): OperationCustomizer = OperationCustomizer { operation, handlerMethod ->
    handlerMethod.preAuthorizeForMethodOrClass()?.let {
      val preAuthExp = SpelExpressionParser().parseExpression(it)
      val evalContext = StandardEvaluationContext()
      evalContext.beanResolver = BeanFactoryResolver(context)
      evalContext.setRootObject(
        object {
          fun hasRole(role: String) = listOf(role)
          fun hasAnyRole(vararg roles: String) = roles.toList()
        },
      )

      val roles = try {
        (preAuthExp.getValue(evalContext) as List<*>).filterIsInstance<String>()
      } catch (e: SpelEvaluationException) {
        emptyList()
      }
      if (roles.isNotEmpty()) {
        operation.description =
          listOf(
            operation.description ?: "",
            "Requires one of the following roles: ",
          ).joinToString(separator = System.lineSeparator()) +
          roles.joinToString(
            prefix = "${System.lineSeparator()}* ",
            separator = "${System.lineSeparator()}* ",
          )
      }
    }
    operation
  }

  private fun HandlerMethod.preAuthorizeForMethodOrClass() = getMethodAnnotation(PreAuthorize::class.java)?.value
    ?: beanType.getAnnotation(PreAuthorize::class.java)?.value
}

@Parameter(
  name = UsernameHeader.NAME,
  `in` = ParameterIn.HEADER,
  description = """
    The username of the user interacting with the client service.
    This can be used instead of the token claim when the client service is acting on behalf of a user.
    When provided, the value passed in the username header will take priority over the subject of the token.
    """,
  required = false,
  content = [Content(schema = Schema(implementation = String::class))],
)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class UsernameHeader {
  companion object {
    const val NAME = "Username"
  }
}
