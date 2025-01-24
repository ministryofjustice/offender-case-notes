package uk.gov.justice.hmpps.casenotes.backfill

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.hmpps.casenotes.alertbackfill.ActiveInactive
import uk.gov.justice.hmpps.casenotes.alertbackfill.AlertCaseNoteVerification
import uk.gov.justice.hmpps.casenotes.alertbackfill.AlertVerificationInformation
import uk.gov.justice.hmpps.casenotes.alertbackfill.CaseNoteAlert
import uk.gov.justice.hmpps.casenotes.alertbackfill.CaseNoteAlertResponse
import uk.gov.justice.hmpps.casenotes.alertbackfill.CodedDescription
import uk.gov.justice.hmpps.casenotes.backfill.AlertsApiExtension.Companion.alertsApi
import uk.gov.justice.hmpps.casenotes.config.ServiceConfig
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import uk.gov.justice.hmpps.casenotes.domain.matchesPersonIdentifier
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.DomainEventListener
import uk.gov.justice.hmpps.casenotes.events.PersonReference
import uk.gov.justice.hmpps.casenotes.health.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.hmpps.casenotes.integrations.UserDetails
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.setByName
import java.time.Instant.ofEpochSecond
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class VerifyAlertCaseNotesIntTest : IntegrationTest() {

  @Autowired
  lateinit var verify: AlertCaseNoteVerification

  @BeforeEach
  fun setup() {
    setActionMissingCaseNotes(true)
  }

  @BeforeEach
  fun reset() {
    setActionMissingCaseNotes(false)
  }

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
      assertThat(acn.createdAt.toLocalDate()).isEqualTo(a.createdAt.toLocalDate())
      assertThat(acn.subType.code).isEqualTo(ActiveInactive.ACTIVE.toString())
      assertThat(acn.authorName).isEqualTo(USER_DETAILS.name)
      assertThat(acn.authorUserId).isEqualTo(USER_DETAILS.userId)
      assertThat(acn.authorUsername).isEqualTo(USER_DETAILS.username)
      if (!a.isActive()) {
        val icn = saved.single { a.inactiveText() == it.text }
        assertThat(icn.createdAt.toLocalDate()).isEqualTo(a.activeTo)
        assertThat(icn.subType.code).isEqualTo(ActiveInactive.INACTIVE.toString())
        assertThat(icn.authorName).isEqualTo(USER_DETAILS.name)
        assertThat(icn.authorUserId).isEqualTo(USER_DETAILS.userId)
        assertThat(icn.authorUsername).isEqualTo(USER_DETAILS.username)
      }
    }
  }

  fun generateCaseNoteAlerts(from: LocalDate, to: LocalDate, count: Int = 5): List<CaseNoteAlert> {
    val dateRange = (from.forRange()..to.forRange())
    val createdAt = ofEpochSecond(dateRange.random()).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val activeFrom = ofEpochSecond(dateRange.random()).atZone(ZoneId.systemDefault()).toLocalDate()
    val activeTo = ofEpochSecond(dateRange.random()).atZone(ZoneId.systemDefault()).toLocalDate()
    return (0..count).map {
      CaseNoteAlert(
        CodedDescription("T$it", "Type $it"),
        CodedDescription("ST$it", "Sub type $it"),
        "CNA",
        activeFrom,
        when {
          it % 3 == 0 -> to.plusDays(3)
          it % 4 == 0 -> activeTo
          else -> null
        },
        createdAt,
      )
    }
  }

  fun generateDomainEvent(
    personIdentifier: String,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): DomainEvent<AlertVerificationInformation> = DomainEvent(
    occurredAt = ZonedDateTime.now(),
    eventType = DomainEventListener.VERIFY_ALERTS,
    detailUrl = null,
    description = "A temporary internal use only event",
    AlertVerificationInformation(personIdentifier, fromDate, toDate),
    PersonReference.withIdentifier(personIdentifier),
  )

  private fun LocalDate.forRange() = toEpochSecond(LocalTime.now(), ZoneOffset.UTC)

  private fun setActionMissingCaseNotes(actionMissingCaseNotes: Boolean) {
    verify.setByName(
      "serviceConfig",
      ServiceConfig(activePrisons = setOf(), baseUrl = "", actionMissingCaseNotes = actionMissingCaseNotes),
    )
  }

  companion object {
    private val USER_DETAILS =
      UserDetails(AlertCaseNoteVerification.DPS_USERNAME, true, "Auto name", "nomis", null, "2", UUID.randomUUID())
  }
}
