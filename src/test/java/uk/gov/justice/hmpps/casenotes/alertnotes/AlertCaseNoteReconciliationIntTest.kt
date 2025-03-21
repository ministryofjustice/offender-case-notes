package uk.gov.justice.hmpps.casenotes.alertnotes

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import uk.gov.justice.hmpps.casenotes.alertnotes.AlertCaseNoteReconciliation.Companion.TYPE
import uk.gov.justice.hmpps.casenotes.alertnotes.AlertsApiExtension.Companion.alertsApi
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.matchesPersonIdentifier
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.DomainEventListener
import uk.gov.justice.hmpps.casenotes.events.PersonReference
import uk.gov.justice.hmpps.casenotes.health.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.hmpps.casenotes.integrations.UserDetails
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import java.time.Instant.ofEpochSecond
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId.systemDefault
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class AlertCaseNoteReconciliationIntTest : IntegrationTest() {

  @Test
  fun `verify creates all case notes when non exist`() {
    val personIdentifier = personIdentifier()
    val from = LocalDate.now().minusDays(30)
    val to = LocalDate.now()

    val alerts = generateCaseNoteAlerts(from, to)
    alertsApi.withAlerts(personIdentifier, from, to, CaseNoteAlertResponse(alerts))

    manageUsersApi.stubGetUserDetails(USER_DETAILS)

    publishEventToTopic(generateDomainEvent(personIdentifier, from, to))

    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    val saved = noteRepository.findAll(matchesPersonIdentifier(personIdentifier))
    assertThat(saved).hasSize(7)
    alerts.forEach { a ->
      val acn = saved.single { a.activeText() == it.text }
      assertThat(acn.occurredAt.toLocalDate()).isEqualTo(a.activeFrom)
      assertThat(acn.createdAt.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(a.createdAt.truncatedTo(ChronoUnit.SECONDS))
      assertThat(acn.subType.code).isEqualTo(ActiveInactive.ACTIVE.name)
      assertThat(acn.authorName).isEqualTo(USER_DETAILS.name)
      assertThat(acn.authorUserId).isEqualTo(USER_DETAILS.userId)
      assertThat(acn.authorUsername).isEqualTo(USER_DETAILS.username)
      if (a.madeInactive()) {
        val icn = saved.single { a.inactiveText() == it.text }
        assertThat(icn.occurredAt.toLocalDate()).isEqualTo(a.activeTo)
        assertThat(icn.createdAt.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(a.madeInactiveAt?.truncatedTo(ChronoUnit.SECONDS))
        assertThat(icn.subType.code).isEqualTo(ActiveInactive.INACTIVE.name)
        assertThat(icn.authorName).isEqualTo(USER_DETAILS.name)
        assertThat(icn.authorUserId).isEqualTo(USER_DETAILS.userId)
        assertThat(icn.authorUsername).isEqualTo(USER_DETAILS.username)
      }
    }
    val sentEvents = hmppsEventsQueue.receivePersonCaseNoteEventsOnQueue()
    assertThat(sentEvents).hasSize(7)
    assertThat(sentEvents.map { it.eventType }.toSet().single()).isEqualTo("person.case-note.created")

    verify(telemetryClient).trackEvent(
      eq("MissingAlertCaseNotes"),
      eq(
        mapOf(
          "PersonIdentifier" to personIdentifier,
          "ActiveInScopeCount" to "6",
          "ActiveInScopeTypes" to alerts.joinToString {
            "${it.type.description} and ${it.subType.description} -> ${it.activeFrom} | ${it.createdAt}"
          },
          "InactiveInScopeCount" to "1",
          "InactiveInScopeTypes" to alerts.filter { it.madeInactive() }.joinToString {
            "${it.type.description} and ${it.subType.description} -> ${it.activeTo} | ${it.madeInactiveAt}"
          },
        ),
      ),
      any(),
    )
  }

  @Test
  fun `verify creates missing case notes when some exist`() {
    val personIdentifier = personIdentifier()
    val from = LocalDate.now().minusDays(30)
    val to = LocalDate.now()

    val main = generateCaseNoteAlerts(from, to)
    val alertWithExisting = main.first { it.subType.code == "ST4" }
    val new = alertWithExisting.copy(
      activeFrom = alertWithExisting.activeTo!!.plusDays(1),
      activeTo = null,
      createdAt = alertWithExisting.createdAt.plusDays(1),
      madeInactiveAt = null,
    )
    val alerts = main + new
    alertsApi.withAlerts(personIdentifier, from, to, CaseNoteAlertResponse(alerts))

    manageUsersApi.stubGetUserDetails(USER_DETAILS)

    val (active, inactive) = getAllTypes().filter { it.type.code == TYPE }.associateBy { it.code }
      .toMap().let { it[ActiveInactive.ACTIVE.name]!! to it[ActiveInactive.INACTIVE.name]!! }
    val existing = listOf(
      givenCaseNote(
        generateCaseNote(
          personIdentifier,
          type = active,
          text = "Alert Type 4 and Sub type 4 made active.",
          createdAt = alertWithExisting.createdAt,
          occurredAt = alertWithExisting.activeFrom.atStartOfDay(),
        ),
      ),
      givenCaseNote(
        generateCaseNote(
          personIdentifier,
          type = inactive,
          text = "Alert Type 4 and Sub type 4 made inactive.",
          createdAt = alertWithExisting.madeInactiveAt,
          occurredAt = alertWithExisting.activeTo.atStartOfDay(),
        ),
      ),
    )

    publishEventToTopic(generateDomainEvent(personIdentifier, from, to))

    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    val saved = noteRepository.findAll(matchesPersonIdentifier(personIdentifier))
    assertThat(saved).hasSize(8)
    assertThat(saved.filter { n -> n.id !in existing.map { it.id } }).hasSize(6)
    val expected = alerts.filter { it.subType.code != "ST4" || it.activeTo == null }
    expected.forEach { it.reconcileWith(saved) }

    val sentEvents = hmppsEventsQueue.receivePersonCaseNoteEventsOnQueue()
    assertThat(sentEvents).hasSize(6)
    assertThat(sentEvents.map { it.eventType }.toSet().single()).isEqualTo("person.case-note.created")

    verify(telemetryClient).trackEvent(
      eq("MissingAlertCaseNotes"),
      eq(
        mapOf(
          "PersonIdentifier" to personIdentifier,
          "ActiveInScopeCount" to "6",
          "ActiveInScopeTypes" to expected.joinToString {
            "${it.type.description} and ${it.subType.description} -> ${it.activeFrom} | ${it.createdAt}"
          },
        ),
      ),
      any(),
    )
  }

  private fun generateCaseNoteAlerts(from: LocalDate, to: LocalDate, count: Int = 5): List<CaseNoteAlert> {
    val dateRange = (from.forRange()..to.minusDays(1).forRange())
    val createdAt = { ofEpochSecond(dateRange.random()).atZone(systemDefault()).toLocalDateTime() }
    val activeFrom = { ofEpochSecond(dateRange.random()).atZone(systemDefault()).toLocalDate() }
    val activeTo = {
      ofEpochSecond((activeFrom().forRange()..to.forRange()).random())
        .atZone(systemDefault()).toLocalDateTime()
    }
    return (0..count).map {
      val activeTo = activeTo()
      CaseNoteAlert(
        CodedDescription("T$it", "Type $it"),
        CodedDescription("ST$it", "Sub type $it"),
        "CNA",
        activeFrom(),
        when {
          it % 3 == 0 -> to.plusDays(3)
          it % 4 == 0 -> activeTo.toLocalDate()
          else -> null
        },
        createdAt(),
        if (it % 3 != 0 && it % 4 == 0) activeTo else null,
      )
    }
  }

  private fun generateDomainEvent(
    personIdentifier: String,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): DomainEvent<AlertReconciliationInformation> = DomainEvent(
    occurredAt = ZonedDateTime.now(),
    eventType = DomainEventListener.RECONCILE_ALERTS,
    detailUrl = null,
    description = "A temporary internal use only event",
    AlertReconciliationInformation(personIdentifier, fromDate, toDate),
    PersonReference.withIdentifier(personIdentifier),
  )

  private fun LocalDate.forRange() = toEpochSecond(LocalTime.now(), ZoneOffset.UTC)

  private fun CaseNoteAlert.reconcileWith(notes: List<Note>) {
    val acn = notes.single { activeText() == it.text && it.occurredAt.toLocalDate() == activeFrom }
    assertThat(acn.createdAt.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(createdAt.truncatedTo(ChronoUnit.SECONDS))
    assertThat(acn.subType.code).isEqualTo(ActiveInactive.ACTIVE.toString())
    assertThat(acn.authorName).isEqualTo(USER_DETAILS.name)
    assertThat(acn.authorUserId).isEqualTo(USER_DETAILS.userId)
    assertThat(acn.authorUsername).isEqualTo(USER_DETAILS.username)
    if (madeInactive()) {
      val icn = notes.single { inactiveText() == it.text }
      assertThat(icn.occurredAt.toLocalDate()).isEqualTo(activeTo)
      assertThat(icn.createdAt.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(madeInactiveAt?.truncatedTo(ChronoUnit.SECONDS))
      assertThat(icn.subType.code).isEqualTo(ActiveInactive.INACTIVE.toString())
      assertThat(icn.authorName).isEqualTo(USER_DETAILS.name)
      assertThat(icn.authorUserId).isEqualTo(USER_DETAILS.userId)
      assertThat(icn.authorUsername).isEqualTo(USER_DETAILS.username)
    }
  }

  companion object {
    private val USER_DETAILS =
      UserDetails(AlertCaseNoteReconciliation.DPS_USERNAME, true, "Auto name", "nomis", null, "2", UUID.randomUUID())
  }
}
