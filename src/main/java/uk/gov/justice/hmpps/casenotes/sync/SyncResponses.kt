package uk.gov.justice.hmpps.casenotes.sync

import java.util.UUID

data class MigrationResult(val id: UUID, val legacyId: Long)
data class SyncResult(val id: UUID, val legacyId: Long, val action: Action) {
  enum class Action { CREATED, UPDATED }
}
