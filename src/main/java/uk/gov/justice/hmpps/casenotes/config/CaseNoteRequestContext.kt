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
    const val USERNAME_HEADER = "Username"
    private const val SYS_USER = "SYS"
    private const val SYS_DISPLAY_NAME = "Sys"
    val NOMIS_CONTEXT = CaseNoteRequestContext(SYS_USER, SYS_DISPLAY_NAME, SYS_USER, source = Source.NOMIS)

    fun get(): CaseNoteRequestContext = RequestContextHolder.getRequestAttributes()
      ?.getAttribute(CaseNoteRequestContext::class.simpleName!!, 0) as CaseNoteRequestContext?
      ?: NOMIS_CONTEXT
  }
}

enum class Source {
  DPS,
  NOMIS,
}
