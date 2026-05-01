package uk.gov.justice.hmpps.casenotes.sar

import java.time.LocalDateTime

data class SarNote(
  val creationDateTime: LocalDateTime,
  val type: String,
  val subType: String,
  val text: String,
  val authorUsername: String,
  val amendments: List<SarAmendment>,
)

data class SarNoteV2(
  val creationDateTime: LocalDateTime,
  val type: String,
  val subType: String,
  val text: String,
  val authorUsername: String,
  val amendments: List<SarAmendment>,
  val location: String,
  val occurredAt: LocalDateTime,
)

data class SarAmendment(val creationDateTime: LocalDateTime, val additionalNoteText: String, val authorUsername: String)
