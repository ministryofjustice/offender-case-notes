package uk.gov.justice.hmpps.casenotes.config

import org.springframework.web.context.request.RequestContextHolder
import java.time.LocalDateTime

data class CaseNoteRequestContext(
  val username: String,
  val userDisplayName: String,
  val activeCaseLoadId: String? = null,
  val source: Source = Source.DPS,
  val requestAt: LocalDateTime = LocalDateTime.now(),
) {
  companion object {
    private const val SYS_USER = "SYS"
    private const val SYS_DISPLAY_NAME = "Sys"
    val NOMIS_CONTEXT = CaseNoteRequestContext(SYS_USER, SYS_DISPLAY_NAME, source = Source.NOMIS)

    fun get(): CaseNoteRequestContext = RequestContextHolder.getRequestAttributes()
      ?.getAttribute(CaseNoteRequestContext::class.simpleName!!, 0) as CaseNoteRequestContext?
      ?: CaseNoteRequestContext(username = SYS_USER, userDisplayName = SYS_DISPLAY_NAME)
  }
}

enum class Source {
  DPS,
  NOMIS,
}
