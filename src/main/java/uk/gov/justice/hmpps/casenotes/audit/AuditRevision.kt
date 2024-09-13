package uk.gov.justice.hmpps.casenotes.audit

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionListener
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.Source
import java.time.LocalDateTime

@Entity
@Table
@RevisionEntity(AuditRevisionEntityListener::class)
class AuditRevision {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @RevisionNumber
  var id: Long = 0

  // must be called timestamp for EnversRevisionRepositoryImpl
  @RevisionTimestamp
  var timestamp: LocalDateTime = LocalDateTime.now()

  var username: String? = null
  var userDisplayName: String? = null
  var caseloadId: String? = null

  @Enumerated(EnumType.STRING)
  var source: Source? = null
}

class AuditRevisionEntityListener : RevisionListener {
  override fun newRevision(revision: Any?) {
    (revision as AuditRevision).apply {
      val context = CaseNoteRequestContext.get()
      username = context.username
      userDisplayName = context.userDisplayName
      caseloadId = context.activeCaseloadId
      source = context.source
    }
  }
}
