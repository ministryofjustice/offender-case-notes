package uk.gov.justice.hmpps.casenotes.domain.audit

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import java.time.LocalDateTime

interface Audited {
  val createDateTime: LocalDateTime
  val createUserId: String
  val modifyDateTime: LocalDateTime?
  val modifyUserId: String?

  fun recordCreatedDetails(context: CaseNoteRequestContext)
  fun recordModifiedDetails(context: CaseNoteRequestContext)
}

@MappedSuperclass
abstract class SimpleAudited(context: CaseNoteRequestContext = CaseNoteRequestContext.get()) : Audited {

  @Column(nullable = false)
  override var createDateTime: LocalDateTime = context.requestAt

  @Column(nullable = false)
  override var createUserId: String = context.username

  @Column
  override var modifyDateTime: LocalDateTime? = null

  @Column
  override var modifyUserId: String? = null

  override fun recordCreatedDetails(context: CaseNoteRequestContext) {
    createDateTime = context.requestAt
    createUserId = context.username
  }

  override fun recordModifiedDetails(context: CaseNoteRequestContext) {
    modifyDateTime = context.requestAt
    modifyUserId = context.username
  }
}
