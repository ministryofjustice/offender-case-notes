// TODO - Delete this
package uk.gov.justice.hmpps.casenotes.sar.tmp

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.sar.SubjectAccessRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.io.BufferedReader

/**
 * Subject Access Request (SAR) template controller returns a mustache template to be used by the SAR service to format
 * the raw SAR data into human-readable format when producing the report PDF. Endpoint is disabled by default.
 *
 * To enable endpoint:
 * - Ensure your service implements one of the [uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestService]
 * interfaces.
 * - Add a template file under your service's resources dir.
 * - Add 'hmpps.sar.template.enabled=true' to your application properties.
 * - Add 'hmpps.sar.template.path=/PATH_TO_YOUR_TEMPLATE' to your application properties.
 *
 * The application will throw an [IllegalStateException] on start up if the feature is enabled but incorrectly
 * configured.
 */
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@Tag(name = "Subject Access Request")
@PreAuthorize("@environment.containsProperty('hmpps.sar.additionalAccessRole') ? hasAnyRole('SAR_DATA_ACCESS', @environment.getProperty('hmpps.sar.additionalAccessRole')) : hasRole('SAR_DATA_ACCESS')")
@RequestMapping("/subject-access-request/template", produces = [MediaType.TEXT_PLAIN_VALUE])
@ConditionalOnBooleanProperty(value = ["hmpps.sar.template.enabled"], havingValue = true)
class HmppsSubjectAccessRequestTemplateController(
  @Value("\${hmpps.sar.template.path:}") val templatePath: String,
) {

  protected companion object {
    private val LOG: Logger = LoggerFactory.getLogger(HmppsSubjectAccessRequestTemplateController::class.java)
    private const val TEMPLATE_PATH_PROPERTY_KEY = "hmpps.sar.template.path"
  }

  @PostConstruct
  fun validateTemplateConfiguration() {
    if (templatePath.isBlank()) {
      throw sarTemplateConfigurationMissingException()
    }

    if (!ClassPathResource(templatePath).exists()) {
      throw sarTemplateConfigurationInvalidException()
    }
  }

  @GetMapping
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Request successfully processed - return template file content",
        content = [
          Content(
            mediaType = "plain/text",
            schema = Schema(implementation = String::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "The client does not have authorisation to make this request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error occurred",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getServiceTemplate(): ResponseEntity<Any> = try {
    getTemplateResource().use {
      ResponseEntity(
        it.readText(),
        HttpHeaders().apply { contentType = MediaType.TEXT_PLAIN },
        HttpStatus.OK,
      )
    }
  } catch (e: Exception) {
    LOG.error("error getting subject access request template: path=$templatePath", e)

    ResponseEntity
      .internalServerError()
      .headers(HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON })
      .body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error getting subject access request template",
          developerMessage = "Unexpected error getting subject access request template",
        ),
      )
  }

  protected fun getTemplateResource(): BufferedReader = templatePath.takeIf { it.isNotBlank() }
    ?.let { this::class.java.getResourceAsStream(templatePath) }
    ?.bufferedReader(Charsets.UTF_8)
    ?: throw RuntimeException("get template resource: $templatePath returned null")

  protected fun sarTemplateConfigurationInvalidException() = IllegalStateException(
    "Invalid subject access request configuration. Configured subject access request template file: " +
      "'$TEMPLATE_PATH_PROPERTY_KEY=$templatePath' not found",
  )

  protected fun sarTemplateConfigurationMissingException() = IllegalStateException(
    "Mandatory configuration blank/missing: HMPPS services implementing the " +
      "${SubjectAccessRequest::class.java.simpleName} interface MUST provide a configuration value for " +
      "'$TEMPLATE_PATH_PROPERTY_KEY'",
  )
}
