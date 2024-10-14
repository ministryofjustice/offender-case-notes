package uk.gov.justice.hmpps.casenotes.sar

import java.time.LocalDateTime

data class SubjectAccessResponse(val prn: String, val content: List<SarNote>)

data class SarNote(
  val creationDateTime: LocalDateTime,
  val type: String,
  val subType: String,
  val text: String,
  val authorName: String,
  val amendments: List<SarAmendment>,
)

data class SarAmendment(val creationDateTime: LocalDateTime, val additionalNoteText: String, val authorName: String)
