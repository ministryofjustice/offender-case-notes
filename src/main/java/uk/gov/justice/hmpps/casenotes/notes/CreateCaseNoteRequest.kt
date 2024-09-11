package uk.gov.justice.hmpps.casenotes.notes

import java.time.LocalDateTime

data class CreateCaseNoteRequest(
  override var locationId: String? = null,
  override val type: String,
  override val subType: String,
  override val occurrenceDateTime: LocalDateTime? = null,
  override val text: String,
  override var systemGenerated: Boolean?,
) : NoteRequest
