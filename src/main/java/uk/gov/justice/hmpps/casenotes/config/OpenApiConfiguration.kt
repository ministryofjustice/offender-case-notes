package uk.gov.justice.hmpps.casenotes.config

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
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
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_READ
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE

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
      ),
    )
    .info(
      Info().title("HMPPS Case Notes API")
        .version(version)
        .description(
          """
            |API for retrieving and managing case notes relating to a person.
            |
            |## Person identifier
            |
            |The accepted `personIdentifier` is the prison number also known as prisoner number, offender number, offender id or NOMS id.
            |
            |## Responsibilities of the client
            |
            |Clients should only allow users to read and write case notes where that user is allowed to view the associated person's information.
            |For example a prisoner should be available to the user's caseload. Contact the Connect DPS team for the latest permissions logic.
            |It is no longer the responsibility of this API to verify the permissions of the user in context as per the move away from passing user tokens.
            |
            |### Sensitive and restricted use case note sub-types
            |
            |The case note sub-types used to categorize case notes have two properties that relate to the visibility of case notes and the sub-type
            |
            |- `sensitive` - the note text may contain sensitive information and therefore should only be displayed to users with one of the `POM`, `VIEW_SENSITIVE_CASE_NOTES` or `ADD_SENSITIVE_CASE_NOTES` DPS roles
            |- `restrictedUse` - the sub-type should only be made available to categorize a new case note to users with either the `POM` or `ADD_SENSITIVE_CASE_NOTES` DPS roles
            |
            |## Case note identifiers and the combined dataset
            |
            |The usage of the combined case notes dataset in DPS was released nationally in February 2025.
            |The combined dataset uses UUIDs for case note identifiers replacing the legacy numeric ids.
            |This is a potentially breaking change for typed clients and therefore they cannot be automatically switched.
            |Instead API clients can 'opt in' to using the combined dataset by including a non empty `""".trimMargin() + CaseloadIdHeader.NAME + """` header value.
            |The presence of this header value declares that the client is:
            |
            |- Following the responsibilities of the client for prisoner visibility, note sensitivity and restricted use sub-types listed above
            |- Compatible with UUID identifiers
            |- Authenticating with a client token containing one or more of the required role claims
            |- Supplying a username for any write endpoints either in the JWT subject or the `""".trimMargin() + UsernameHeader.NAME + """` header
            |
            |## Authentication
            |
            |This API uses OAuth2 with JWTs. You will need to pass the JWT in the `Authorization` header using the `Bearer` scheme.
            |All endpoints are designed to work with client tokens and user tokens should not be used with this service.
            |
            |## Authorisation
            |
            |The API uses roles to control access to the endpoints. The roles required for each endpoint are documented in the endpoint descriptions.
            |Services integrating with the API should request one of the two following roles depending on their needs:
            |
            |1. `""".trimMargin() + ROLE_CASE_NOTES_READ + """` - Grants read only access to the API e.g. retrieving case notes for a person
            |2. `""".trimMargin() + ROLE_CASE_NOTES_WRITE + """` - Grants read/write access to the API e.g. creating case notes and adding amendments
            |
            |**IMPORTANT** clients should never request the admin role or call admin only endpoints e.g. delete
            |
            |## Identifying the user
            |
            |Endpoints that modify case notes require the user to be identified via their username.
            |This is to correctly populate the case note author and for auditing purposes.
            |The username for the request can be supplied in two ways:
            |
            |1. **Token claim** - Via a `subject` claim in the JWT
            |2. **Header** - Via the '""".trimMargin() + UsernameHeader.NAME + """' header which will take priority over 1.
            |
            |Where possible clients are expected to use the token claim subject to supply the username.            |
          """.trimMargin(),
        ).contact(
          Contact()
            .name("HMPPS Digital Studio")
            .email("feedback@digital.justice.gov.uk"),
        ),
    )
    .components(
      Components().addSecuritySchemes(
        "bearer-jwt",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
          .`in`(SecurityScheme.In.HEADER)
          .name("Authorization"),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt", listOf("read", "write")))

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
  name = CaseloadIdHeader.NAME,
  `in` = ParameterIn.HEADER,
  description = """
    Relevant caseload id for the client identity in context e.g. the active caseload id of the logged in user.
    Used to declare that the client is compatible with the usage of the combined case notes dataset.
    """,
  required = false,
  content = [Content(schema = Schema(implementation = String::class))],
)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class CaseloadIdHeader {
  companion object {
    const val NAME = "CaseloadId"
  }
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
