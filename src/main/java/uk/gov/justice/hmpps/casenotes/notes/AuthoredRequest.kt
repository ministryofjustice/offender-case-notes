package uk.gov.justice.hmpps.casenotes.notes

interface AuthoredRequest {
  val authorUsername: String?
  val authorName: String
}
