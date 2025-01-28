package uk.gov.justice.hmpps.casenotes.alertbackfill

import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime

data class CaseNoteAlertResponse(val content: List<CaseNoteAlert>)

data class CaseNoteAlert(
  val type: CodedDescription,
  val subType: CodedDescription,
  val prisonCode: String?,
  val activeFrom: LocalDate,
  val activeTo: LocalDate?,
  val createdAt: LocalDateTime,
  val madeInactiveAt: LocalDateTime?,
) {
  fun isInactive() = activeTo != null && !activeTo.isAfter(now())
  fun madeInactive(): Boolean = isInactive() && madeInactiveAt != null

  private fun baseText() = "Alert ${type.description} and ${subType.description} made"
  fun activeText() = "${baseText()} active."
  fun inactiveText() = "${baseText()} inactive."
}

data class CodedDescription(val code: String, val description: String)
