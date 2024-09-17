package uk.gov.justice.hmpps.casenotes.domain.audit

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import java.time.LocalDateTime

interface Created {
  val createdAt: LocalDateTime
  val createdBy: String

  fun recordCreatedDetails(context: CaseNoteRequestContext)
}

@MappedSuperclass
abstract class SimpleAudited(context: CaseNoteRequestContext = CaseNoteRequestContext.get()) : Created {

  @field:Column(nullable = false)
  override var createdAt: LocalDateTime = context.requestAt

  @field:Column(nullable = false)
  override var createdBy: String = context.username

  override fun recordCreatedDetails(context: CaseNoteRequestContext) {
    createdAt = context.requestAt
    createdBy = context.username
  }
}
