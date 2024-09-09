package uk.gov.justice.hmpps.casenotes.notes

import java.time.LocalDateTime

interface AuthoredRequest {
  val authorUsername: String
  val authorUserId: String
  val authorName: String
  val createdDateTime: LocalDateTime
}
