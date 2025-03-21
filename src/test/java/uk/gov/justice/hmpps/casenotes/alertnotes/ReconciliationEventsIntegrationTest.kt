package uk.gov.justice.hmpps.casenotes.alertnotes

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import uk.gov.justice.hmpps.casenotes.alertnotes.AlertsApiExtension.Companion.alertsApi
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

class ReconciliationEventsIntegrationTest : IntegrationTest() {
  @Test
  fun `can generate reconciliation events for yesterday`() {
    val from = LocalDate.now().minusDays(1)
    val created = from.atTime(LocalTime.now())
    val to = LocalDate.now()
    val prisonNumbers = arrayOf(personIdentifier(), personIdentifier())
    alertsApi.withPersonIdentifiers(from, to, *prisonNumbers)
    prisonNumbers.map { alertsApi.withAlerts(it, from, to, response(from, created)) }

    webTestClient.post().uri("/case-notes/alerts/reconciliation")
      .exchange()
      .expectStatus().isNoContent

    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    prisonNumbers.forEach {
      verify(telemetryClient).trackEvent(
        "MissingActiveAlertCaseNote",
        mapOf(
          "personIdentifier" to it,
          "type" to "Type description",
          "subType" to "Sub type description",
          "from" to ISO_DATE.format(from),
          "createdAt" to ISO_LOCAL_DATE_TIME.format(created),
        ),
        mapOf(),
      )
    }
  }

  private fun response(from: LocalDate, created: LocalDateTime) = CaseNoteAlertResponse(
    listOf(
      CaseNoteAlert(
        CodedDescription("TYPE", "Type description"),
        CodedDescription("SUB", "Sub type description"),
        "CNA",
        from,
        null,
        created,
        null,
      ),
    ),
  )
}
