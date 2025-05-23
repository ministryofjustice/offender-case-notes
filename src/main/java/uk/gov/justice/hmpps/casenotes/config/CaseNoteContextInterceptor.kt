package uk.gov.justice.hmpps.casenotes.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.Span
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ValidationException
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.hmpps.casenotes.integrations.ManageUsersService
import uk.gov.justice.hmpps.casenotes.legacy.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.legacy.dto.UserDetails.Companion.NOMIS

@Configuration
class CaseNoteContextConfiguration(private val caseNoteContextInterceptor: CaseNoteContextInterceptor) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(caseNoteContextInterceptor)
      .addPathPatterns("/case-notes/**", "/admin/case-notes/**")
      .excludePathPatterns("/case-notes/usage", "/case-notes/staff-usage", "/case-notes/alerts/reconciliation")
  }
}

@Configuration
class CaseNoteContextInterceptor(
  private val manageUserService: ManageUsersService,
  private val objectMapper: ObjectMapper,
  private val serviceConfig: ServiceConfig,
) : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (request.method in listOf(POST.name(), PUT.name(), PATCH.name(), DELETE.name())) {
      val headers = request.extractHeaders()
      val username = headers?.username ?: username()
      return manageUserService.getUserDetails(username)?.let {
        val context = CaseNoteRequestContext(
          username,
          it.name,
          it.userId,
          it.activeCaseLoadId,
          Source.DPS,
          nomisUser = it.authSource == NOMIS,
        )
        request.setAttribute(CaseNoteRequestContext::class.simpleName, context)
        true
      } ?: response.handleNoUserDetails(objectMapper)
    }
    return true
  }

  private fun HttpServletResponse.handleNoUserDetails(objectMapper: ObjectMapper): Boolean {
    status = HttpStatus.BAD_REQUEST.value()
    contentType = org.springframework.http.MediaType.APPLICATION_JSON_VALUE
    writer.write(
      objectMapper.writeValueAsString(
        ErrorResponse(
          HttpStatus.BAD_REQUEST.value(),
          developerMessage = "Invalid username provided in token",
        ),
      ),
    )
    return false
  }

  private fun authentication(): AuthAwareAuthenticationToken = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
    ?: throw AccessDeniedException("User is not authenticated")

  private fun username(): String = authentication().name.takeIf { it.length <= 64 }
    ?: throw ValidationException("username for audit exceeds 64 characters")

  private fun HttpServletRequest.extractHeaders(): Headers? {
    return getHeader(CaseloadIdHeader.NAME)?.let {
      if (it.isBlank()) return null
      // add request header to request in app insights to avoid having custom events
      Span.current().setAttribute("caseloadId", it)
      // using usernameHeader as the username property is already set as part of client tracking config (user_name)
      val username = getHeader(UsernameHeader.NAME)?.also { Span.current().setAttribute("usernameHeader", it) }
      // only allow username header for switched path
      if (serviceConfig.switchesPathFor(it)) {
        Headers(it, username)
      } else {
        null
      }
    }
  }
}

private data class Headers(val caseloadId: String, val username: String?)
