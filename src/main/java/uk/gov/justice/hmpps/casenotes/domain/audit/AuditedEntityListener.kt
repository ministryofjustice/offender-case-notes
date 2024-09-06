package uk.gov.justice.hmpps.casenotes.domain.audit

import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.Source

class AuditedEntityListener {

  @PreUpdate
  fun onPreUpdate(auditable: Audited) {
    auditable.recordModifiedDetails()
  }

  @PreRemove
  fun onPreRemove(auditable: Audited) {
    auditable.recordModifiedDetails()
  }

  private fun Audited.recordModifiedDetails() {
    val context = CaseNoteRequestContext.get()
    if (context.source == Source.DPS) {
      recordModifiedDetails(context)
    }
  }
}
