package uk.gov.justice.hmpps.casenotes.alertnotes

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Alert(
  val alertUuid: UUID,
  val prisonNumber: String,
  val alertCode: AlertCodeSummary,
  val activeFrom: LocalDate,
  val activeTo: LocalDate?,
  val isActive: Boolean,
  val createdAt: LocalDateTime,
  val createdBy: String,
  val activeToLastSetAt: LocalDateTime?,
  val activeToLastSetBy: String?,
) {
  private fun textTemplate() = "Alert ${alertCode.alertTypeDescription} and ${alertCode.description} made"
  fun activeText() = "${textTemplate()} active."
  fun inactiveText() = "${textTemplate()} inactive."

  fun inactiveUsername(): String? = if (activeTo == activeToLastSetAt?.toLocalDate()) activeToLastSetBy else null
}

data class AlertCodeSummary(
  val alertTypeCode: String,
  val alertTypeDescription: String,
  val code: String,
  val description: String,
)
