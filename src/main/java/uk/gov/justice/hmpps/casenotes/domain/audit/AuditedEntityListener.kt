package uk.gov.justice.hmpps.casenotes.domain.audit

import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext

class AuditedEntityListener {

  @PreUpdate
  fun onPreUpdate(auditable: Audited) {
    auditable.recordModifiedDetails(CaseNoteRequestContext.get())
  }

  @PreRemove
  fun onPreRemove(auditable: Audited) {
    auditable.recordModifiedDetails(CaseNoteRequestContext.get())
  }
}
