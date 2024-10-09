package uk.gov.justice.hmpps.casenotes.legacy.dto

class UserDetails(
  val username: String? = null,
  val active: Boolean? = null,
  val name: String? = null,
  val activeCaseLoadId: String? = null,
  val userId: String? = null,
  val authSource: String? = null,
) {
  companion object {
    const val NOMIS = "nomis"
  }
}
