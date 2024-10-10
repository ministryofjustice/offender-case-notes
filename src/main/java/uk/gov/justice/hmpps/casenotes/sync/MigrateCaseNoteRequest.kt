package uk.gov.justice.hmpps.casenotes.sync

import uk.gov.justice.hmpps.casenotes.domain.System
import java.time.LocalDateTime

data class MigrateCaseNoteRequest(
  override val legacyId: Long,
  override val locationId: String,
  override val type: String,
  override val subType: String,
  override val occurrenceDateTime: LocalDateTime,
  override val text: String,
  override val systemGenerated: Boolean,
  override val system: System,
  override val author: Author,
  override val createdDateTime: LocalDateTime,
  override val createdByUsername: String,
  override val amendments: Set<MigrateAmendmentRequest>,
) : SyncNoteRequest, SystemAwareRequest

data class MigrateAmendmentRequest(
  override val text: String,
  override val system: System,
  override val author: Author,
  override val createdDateTime: LocalDateTime,
) : SyncAmendmentRequest, SystemAwareRequest
