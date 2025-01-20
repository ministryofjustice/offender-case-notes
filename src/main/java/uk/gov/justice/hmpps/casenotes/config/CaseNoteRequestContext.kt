package uk.gov.justice.hmpps.casenotes.config

import org.springframework.web.context.request.RequestContextHolder
import java.time.LocalDateTime

data class CaseNoteRequestContext(
  val username: String,
  val userDisplayName: String,
  val userId: String,
  val activeCaseloadId: String? = null,
  val source: Source = Source.DPS,
  val requestAt: LocalDateTime = LocalDateTime.now(),
  val nomisUser: Boolean = false,
) {
  constructor(username: String) : this(username, username, username)

  companion object {
    private const val SYS_USER = "SYS"
    private const val SYS_DISPLAY_NAME = "Sys"
    private val NomisContext = { CaseNoteRequestContext(SYS_USER, SYS_DISPLAY_NAME, SYS_USER, source = Source.NOMIS) }

    @JvmStatic
    fun get(): CaseNoteRequestContext = RequestContextHolder.getRequestAttributes()
      ?.getAttribute(CaseNoteRequestContext::class.simpleName!!, 0) as CaseNoteRequestContext?
      ?: NomisContext()
  }
}

enum class Source {
  DPS,
  NOMIS,
}
