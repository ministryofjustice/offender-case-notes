package uk.gov.justice.hmpps.casenotes.sync

import java.util.UUID

data class SyncResult(val id: UUID, val legacyId: Long, val action: Action) {
  enum class Action { CREATED, UPDATED }
}
