package uk.gov.justice.hmpps.casenotes.alertnotes

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import uk.gov.justice.hmpps.casenotes.alertnotes.ActiveInactive.ACTIVE
import uk.gov.justice.hmpps.casenotes.alertnotes.ActiveInactive.INACTIVE
import uk.gov.justice.hmpps.casenotes.alertnotes.AlertCaseNoteHandler.AlertAdditionalInformation
import uk.gov.justice.hmpps.casenotes.alertnotes.AlertsApiExtension.Companion.alertsApi
import uk.gov.justice.hmpps.casenotes.config.EuropeLondon
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import uk.gov.justice.hmpps.casenotes.domain.IdGenerator.newUuid
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.DomainEventListener.Companion.ALERT_CREATED
import uk.gov.justice.hmpps.casenotes.events.DomainEventListener.Companion.ALERT_INACTIVE
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent
import uk.gov.justice.hmpps.casenotes.events.PersonReference
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension.Companion.elite2Api
import uk.gov.justice.hmpps.casenotes.health.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.hmpps.casenotes.health.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.hmpps.casenotes.integrations.PrisonDetail
import uk.gov.justice.hmpps.casenotes.integrations.UserDetails
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class AlertCaseNoteIntegrationTest : IntegrationTest() {

  @Test
  fun `no alert active case note created if prison switch endpoint returns 404`() {
    val prisonCode = "ANY"
    val alert = alert()
    alertsApi.withAlert(alert)
    prisonerSearchApi.stubPrisonerDetails(alert.prisonNumber, prisonCode)
    elite2Api.stubPrisonSwitchNotFound()

    publishEventToTopic(alert.domainEvent(ALERT_CREATED))
    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    verify(noteRepository, never()).save(any())
  }

  @Test
  fun `no alert active case note created if prison not active`() {
    val prisonCode = "INA"
    val alert = alert()
    alertsApi.withAlert(alert)
    prisonerSearchApi.stubPrisonerDetails(alert.prisonNumber, prisonCode)
    elite2Api.stubPrisonSwitch(response = listOf(PrisonDetail("OTH", "Any other prison")))

    publishEventToTopic(alert.domainEvent(ALERT_CREATED))
    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    verify(noteRepository, never()).save(any())
  }

  @Test
  fun `alert active case note created if prison active`() {
    val prisonCode = "ACT"
    val alert = alert()
    alertsApi.withAlert(alert)
    prisonerSearchApi.stubPrisonerDetails(alert.prisonNumber, prisonCode)
    elite2Api.stubPrisonSwitch(response = listOf(PrisonDetail("ACT", "Active Prison")))

    val userDetails = UserDetails(alert.createdBy, true, "Brian Created", "nomis", "null", "5761427", newUuid())
    manageUsersApi.stubGetUserDetails(userDetails)

    publishEventToTopic(alert.domainEvent(ALERT_CREATED))
    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    val caseNote = noteRepository.findAll().single {
      it.personIdentifier == alert.prisonNumber && it.locationId == prisonCode
    }

    caseNote.verifyAgainst(alert, userDetails)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, caseNote)
  }

  @Test
  fun `alert active case note created if all prisons active`() {
    val prisonCode = "SOM"
    val alert = alert(createdBy = "AN07H3R")
    alertsApi.withAlert(alert)
    prisonerSearchApi.stubPrisonerDetails(alert.prisonNumber, prisonCode)
    elite2Api.stubPrisonSwitch(
      response = listOf(
        PrisonDetail("OTH", "Any other prison"),
        PrisonDetail("*ALL*", "Active Prison"),
      ),
    )

    val userDetails = UserDetails(alert.createdBy, true, "Another Person", "nomis", "null", "81946582", newUuid())
    manageUsersApi.stubGetUserDetails(userDetails)

    publishEventToTopic(alert.domainEvent(ALERT_CREATED))
    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    val caseNote = noteRepository.findAll().single {
      it.personIdentifier == alert.prisonNumber && it.locationId == prisonCode
    }

    caseNote.verifyAgainst(alert, userDetails)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, caseNote)
  }

  @Test
  fun `alert inactive case note created if alert created inactive`() {
    val prisonCode = "CRI"
    val alert = alert(
      activeTo = LocalDate.now().minusDays(1),
      createdBy = "AN07H3R",
      madeInactiveAt = LocalDateTime.now(),
      madeInactiveBy = "AN07H3R",
    )
    alertsApi.withAlert(alert)
    prisonerSearchApi.stubPrisonerDetails(alert.prisonNumber, prisonCode)
    elite2Api.stubPrisonSwitch(response = listOf(PrisonDetail("*ALL*", "All Active")))

    val userDetails = UserDetails(alert.createdBy, true, "Another Person", "nomis", "null", "81946582", newUuid())
    manageUsersApi.stubGetUserDetails(userDetails)

    publishEventToTopic(alert.domainEvent(ALERT_CREATED))
    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    val caseNote = noteRepository.findAll().single {
      it.personIdentifier == alert.prisonNumber && it.locationId == prisonCode
    }

    caseNote.verifyAgainst(alert, userDetails)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, caseNote)
  }

  @Test
  fun `no alert inactive case note created if prison not active`() {
    val prisonCode = "INA"
    val alert = alert(activeTo = LocalDate.now(), activeToLastSetAt = LocalDateTime.now())
    alertsApi.withAlert(alert)
    prisonerSearchApi.stubPrisonerDetails(alert.prisonNumber, prisonCode)
    elite2Api.stubPrisonSwitch(response = listOf(PrisonDetail("OTH", "Any other prison")))

    publishEventToTopic(alert.domainEvent(ALERT_CREATED))
    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    verify(noteRepository, never()).save(any())
  }

  @Test
  fun `alert inactive case note created if prison active`() {
    val prisonCode = "ACT"
    val alert =
      alert(activeTo = LocalDate.now(), madeInactiveAt = LocalDateTime.now(), madeInactiveBy = "BCreated")
    alertsApi.withAlert(alert)
    prisonerSearchApi.stubPrisonerDetails(alert.prisonNumber, prisonCode)
    elite2Api.stubPrisonSwitch(response = listOf(PrisonDetail("ACT", "Active Prison")))

    val userDetails =
      UserDetails(alert.madeInactiveBy!!, true, "Brian Created", "nomis", "null", "5761427", newUuid())
    manageUsersApi.stubGetUserDetails(userDetails)

    val event = alert.domainEvent(ALERT_INACTIVE)
    publishEventToTopic(event)
    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    val caseNote = noteRepository.findAll().single {
      it.personIdentifier == alert.prisonNumber && it.locationId == prisonCode
    }

    caseNote.verifyAgainst(alert, userDetails)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, caseNote)
  }

  @Test
  fun `alert inactive case note created if all prisons active`() {
    val prisonCode = "SOM"
    val alert =
      alert(activeTo = LocalDate.now(), madeInactiveAt = LocalDateTime.now(), madeInactiveBy = "AN07H3R")
    alertsApi.withAlert(alert)
    prisonerSearchApi.stubPrisonerDetails(alert.prisonNumber, prisonCode)
    elite2Api.stubPrisonSwitch(
      response = listOf(
        PrisonDetail("OTH", "Any other prison"),
        PrisonDetail("*ALL*", "Active Prison"),
      ),
    )

    val userDetails =
      UserDetails(alert.madeInactiveBy!!, true, "Another Person", "nomis", "null", "81946582", newUuid())
    manageUsersApi.stubGetUserDetails(userDetails)

    val event = alert.domainEvent(ALERT_INACTIVE)
    publishEventToTopic(event)
    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    val caseNote = noteRepository.findAll().single {
      it.personIdentifier == alert.prisonNumber && it.locationId == prisonCode
    }

    caseNote.verifyAgainst(alert, userDetails)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, caseNote)
  }

  @Test
  fun `alert inactive case note created when automatically made inactive`() {
    val prisonCode = "ACT"
    val alert = alert(activeTo = LocalDate.now(), madeInactiveAt = LocalDateTime.now(), madeInactiveBy = "deactiv8")
    alertsApi.withAlert(alert)
    prisonerSearchApi.stubPrisonerDetails(alert.prisonNumber, prisonCode)
    elite2Api.stubPrisonSwitch(
      response = listOf(PrisonDetail("ACT", "Active Prison"), PrisonDetail("*ALL*", "Active Prison")),
    )

    val event = alert.domainEvent(ALERT_INACTIVE)
    publishEventToTopic(event)
    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    val caseNote = noteRepository.findAll().single {
      it.personIdentifier == alert.prisonNumber && it.locationId == prisonCode
    }

    caseNote.verifyAgainst(
      alert,
      UserDetails("OMS_OWNER", true, "System Generated", "nomis", null, "1", null),
    )

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, caseNote)
  }
}

fun Alert.domainEvent(eventName: String): DomainEvent<AlertAdditionalInformation> = DomainEvent(
  occurredAt = activeToLastSetAt?.atZone(EuropeLondon) ?: ZonedDateTime.now(EuropeLondon),
  eventName,
  null,
  "An alert was created or made inactive",
  AlertAdditionalInformation(alertUuid),
  PersonReference.withIdentifier(prisonNumber),
)

fun alert(
  prisonNumber: String = personIdentifier(),
  alertCode: AlertCodeSummary = AlertCodeSummary("TC1", "Type Description", "C1", "Description"),
  activeFrom: LocalDate = LocalDate.now().minusDays(30),
  activeTo: LocalDate? = null,
  createdAt: LocalDateTime = LocalDateTime.now().minusDays(28),
  createdBy: String = "BCreated",
  activeToLastSetAt: LocalDateTime? = null,
  activeToLastSetBy: String? = null,
  madeInactiveAt: LocalDateTime? = null,
  madeInactiveBy: String? = null,
  alertUuid: UUID = newUuid(),
) = Alert(
  alertUuid,
  prisonNumber,
  alertCode,
  activeFrom,
  activeTo,
  activeTo == null || activeTo.isAfter(LocalDate.now()),
  createdAt,
  createdBy,
  activeToLastSetAt,
  activeToLastSetBy,
  madeInactiveAt,
  madeInactiveBy,
)

private fun Note.verifyAgainst(alert: Alert, userDetails: UserDetails) {
  assertThat(createdAt.truncatedTo(SECONDS)).isEqualTo(alert.createdAt()!!.truncatedTo(SECONDS))
  assertThat(ActiveInactive.valueOf(subType.code)).isEqualTo(if (alert.isActive) ACTIVE else INACTIVE)
  assertThat(authorUsername).isEqualTo(userDetails.username)
  assertThat(authorUserId).isEqualTo(userDetails.userId)
  assertThat(occurredAt.toLocalDate()).isEqualTo(if (alert.isActive) alert.activeFrom else alert.activeTo)
  assertThat(text).isEqualTo(if (alert.isActive) alert.activeText() else alert.inactiveText())
}

private fun Alert.createdAt() = if (isActive) createdAt else madeInactiveAt
