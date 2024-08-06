package uk.gov.justice.hmpps.casenotes.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext.Companion.NOMIS_CONTEXT
import uk.gov.justice.hmpps.casenotes.dto.UserDetails
import uk.gov.justice.hmpps.casenotes.services.ExternalApiService

@Configuration
class CaseNoteContextConfiguration(private val caseNoteContextInterceptor: CaseNoteContextInterceptor) :
  WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(caseNoteContextInterceptor).addPathPatterns("/case-notes/**")
    registry.addInterceptor(caseNoteContextInterceptor).addPathPatterns("/case-notes/**/**")
    registry.addInterceptor(caseNoteContextInterceptor).addPathPatterns("/case-notes/amendment/**/**")
  }
}

@Configuration
class CaseNoteContextInterceptor(private val externalApiService: ExternalApiService) : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (request.method in listOf(POST.name(), PUT.name(), PATCH.name(), DELETE.name())) {
      val source = request.source()
      val context = request.userDetails()?.let {
        CaseNoteRequestContext(it.username!!, it.name!!, it.userId ?: it.username, it.activeCaseLoadId, source)
      }

      check(context != null || source == Source.NOMIS)

      request.setAttribute(
        CaseNoteRequestContext::class.simpleName,
        context ?: NOMIS_CONTEXT,
      )
    }

    return true
  }

  private fun HttpServletRequest.source(): Source =
    getHeader(SOURCE)?.let { Source.valueOf(it) } ?: Source.DPS

  private fun authentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw AccessDeniedException("User is not authenticated")

  private fun HttpServletRequest.username(): String = when (source()) {
    Source.DPS -> authentication().name
    Source.NOMIS -> getHeader(USERNAME)?.trim() ?: Source.NOMIS.name
  }

  private fun HttpServletRequest.userDetails(): UserDetails? =
    externalApiService.getUserDetails(username()).orElse(null)

  companion object {
    private const val USERNAME = "Username"
    private const val SOURCE = "Source"
  }
}
