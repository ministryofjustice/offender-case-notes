package uk.gov.justice.hmpps.casenotes.types.internal

import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext

class AuditedEntityListener {

  @PrePersist
  fun onPrePersist(auditable: Audited) {
    auditable.recordCreatedDetails(CaseNoteRequestContext.get())
  }

  @PreUpdate
  fun onPreUpdate(auditable: Audited) {
    auditable.recordModifiedDetails(CaseNoteRequestContext.get())
  }
}
