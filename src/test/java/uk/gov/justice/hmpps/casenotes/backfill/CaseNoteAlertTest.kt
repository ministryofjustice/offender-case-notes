package uk.gov.justice.hmpps.casenotes.backfill

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.casenotes.alertbackfill.CaseNoteAlert
import uk.gov.justice.hmpps.casenotes.alertbackfill.CodedDescription
import java.time.LocalDateTime

class CaseNoteAlertTest {

  private val oncrType = CodedDescription("O", "Other")
  private val cpcType = CodedDescription("C", "Child Communication Measures")

  private val oncrSubType = CodedDescription("ONCR", "Public Protection Contact Restriction")
  private val cpcSubType = CodedDescription("CPC", "PPRC and Child Contact Arrangements")

  private val location = "LEI"
  private val beforeChanges = LocalDateTime.parse("2024-11-25T09:42:48.526352")
  private val afterChanges = LocalDateTime.parse("2024-11-25T10:02:17.547652")

  @Test
  fun `text for oncr before and after change`() {
    val after = caseNoteAlert(oncrType, oncrSubType, afterChanges)
    val before = caseNoteAlert(oncrType, oncrSubType, beforeChanges)

    assertThat(after.activeText()).isEqualTo("Alert Other and Public Protection Contact Restriction made active.")
    assertThat(before.activeText()).isEqualTo(after.activeText())
    assertThat(after.inactiveText()).isEqualTo("Alert Other and Public Protection Contact Restriction made inactive.")
    assertThat(before.inactiveText()).isEqualTo(after.inactiveText())
    assertThat(before.alternativeActiveText()).isEqualTo("Alert Other and No-contact request made active.")
    assertThat(before.alternativeInactiveText()).isEqualTo("Alert Other and No-contact request made inactive.")
  }

  @Test
  fun `text for cpc before and after change`() {
    val after = caseNoteAlert(cpcType, cpcSubType, afterChanges)
    val before = caseNoteAlert(cpcType, cpcSubType, beforeChanges)

    assertThat(after.activeText()).isEqualTo("Alert Child Communication Measures and PPRC and Child Contact Arrangements made active.")
    assertThat(before.activeText()).isEqualTo(after.activeText())
    assertThat(after.inactiveText()).isEqualTo("Alert Child Communication Measures and PPRC and Child Contact Arrangements made inactive.")
    assertThat(before.inactiveText()).isEqualTo(after.inactiveText())
    assertThat(before.alternativeActiveText()).isEqualTo("Alert Child Communication Measures and PPRC made active.")
    assertThat(before.alternativeInactiveText()).isEqualTo("Alert Child Communication Measures and PPRC made inactive.")
  }

  private fun caseNoteAlert(type: CodedDescription, subType: CodedDescription, dateTime: LocalDateTime) = CaseNoteAlert(type, subType, location, dateTime.toLocalDate(), dateTime.toLocalDate(), dateTime, dateTime)
}
