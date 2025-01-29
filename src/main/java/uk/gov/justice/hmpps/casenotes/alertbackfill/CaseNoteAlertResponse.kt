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

  private fun textTemplate(typeDescription: String, subTypeDescription: String) =
    "Alert $typeDescription and $subTypeDescription made"

  private fun baseText(dateTime: LocalDateTime?) = when (type.code to subType.code) {
    "C" to "CPC" if (dateTime?.isBefore(cpcDateUpdated) == true) -> textTemplate(type.description, "PPRC")
    "O" to "ONCR" if (dateTime?.isBefore(oncrDateUpdated) == true) -> textTemplate(type.description, "No-contact request")
    else -> textTemplate(type.description, subType.description)
  }

  fun activeText(): String = "${baseText(createdAt)} active."
  fun inactiveText(): String = "${baseText(madeInactiveAt)} inactive."
  fun alternativeActiveText() = "${baseText(LocalDateTime.now())} active."
  fun alternativeInactiveText() = "${baseText(LocalDateTime.now())} inactive."

  private val cpcDateUpdated = LocalDateTime.parse("2024-11-25T09:54:57.787788")
  private val oncrDateUpdated = LocalDateTime.parse("2024-11-25T09:54:29.816785")
}

data class CodedDescription(val code: String, val description: String)
