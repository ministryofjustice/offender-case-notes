package uk.gov.justice.hmpps.casenotes.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ValidationException
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.dto.UserDetails.Companion.NOMIS
import uk.gov.justice.hmpps.casenotes.services.ExternalApiService

@Configuration
class CaseNoteContextConfiguration(private val caseNoteContextInterceptor: CaseNoteContextInterceptor) :
  WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(caseNoteContextInterceptor)
      .addPathPatterns("/case-notes/**")
      .addPathPatterns("/case-notes/**/**")
      .addPathPatterns("/case-notes/amendment/**/**")
  }
}

@Configuration
class CaseNoteContextInterceptor(
  private val externalApiService: ExternalApiService,
  private val objectMapper: ObjectMapper,
) : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (request.method in listOf(POST.name(), PUT.name(), PATCH.name(), DELETE.name())) {
      val username = request.username()
      return externalApiService.getUserDetails(username)?.let {
        val context = CaseNoteRequestContext(
          username,
          it.name ?: username,
          it.userId ?: username,
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
    status = org.springframework.http.HttpStatus.BAD_REQUEST.value()
    contentType = org.springframework.http.MediaType.APPLICATION_JSON_VALUE
    writer.write(
      objectMapper.writeValueAsString(
        ErrorResponse(
          org.springframework.http.HttpStatus.BAD_REQUEST.value(),
          developerMessage = "Invalid username provided in token",
        ),
      ),
    )
    return false
  }

  private fun authentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw AccessDeniedException("User is not authenticated")

  private fun HttpServletRequest.username(): String =
    (getHeader(USERNAME) ?: authentication().name).takeIf { it.length <= 64 }
      ?: throw ValidationException("username for audit exceeds 64 characters")

  companion object {
    private const val USERNAME = "Username"
  }
}
