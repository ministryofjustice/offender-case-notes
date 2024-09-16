package uk.gov.justice.hmpps.casenotes.sync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.hmpps.casenotes.config.Source
import java.time.LocalDateTime
import java.util.UUID

data class SyncCaseNoteRequest(
  override val legacyId: Long,

  @Schema(
    example = "c9475622-676f-4659-8bb5-12a4760280d7",
    description = "The id for the case note, if provided, the existing case note matching this id will be updated, otherwise a new case note is created.",
  )
  val id: UUID?,

  override val personIdentifier: String,
  override val locationId: String,
  override val type: String,
  override val subType: String,
  override val occurrenceDateTime: LocalDateTime,
  override val text: String,
  override val systemGenerated: Boolean,
  override val author: CreateAuthor,
  override val createdDateTime: LocalDateTime,
  override val createdByUsername: String,
  override val source: Source,
  override val amendments: Set<SyncCaseNoteAmendmentRequest>,
) : SyncNoteRequest

data class SyncCaseNoteAmendmentRequest(
  override val text: String,
  override val author: AmendAuthor,
  override val createdDateTime: LocalDateTime,
) : SyncAmendmentRequest
