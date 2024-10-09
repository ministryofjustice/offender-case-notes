package uk.gov.justice.hmpps.casenotes.services

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import uk.gov.justice.hmpps.casenotes.domain.matchesPersonIdentifier
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.DomainEventListener.Companion.PRISONER_MERGED
import uk.gov.justice.hmpps.casenotes.events.MergeInformation
import uk.gov.justice.hmpps.casenotes.events.PersonReference
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import java.time.ZonedDateTime

class MergeEventIntTest : IntegrationTest() {
  @Test
  fun `receiving a merge event updates person identifier`() {
    val oldNoms = personIdentifier()
    givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(2)

    val newNoms = personIdentifier()
    publishEventToTopic(mergeIdentifiers(oldNoms, newNoms))

    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    assertThat(noteRepository.findAll(matchesPersonIdentifier(newNoms)).size).isEqualTo(2)
    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(0)
  }

  @Test
  fun `receiving a merge event merges all existing case notes`() {
    val oldNoms = personIdentifier()
    givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(2)

    val newNoms = personIdentifier()
    givenCaseNote(generateCaseNote(newNoms).withAmendment().withAmendment())
    assertThat(noteRepository.findAll(matchesPersonIdentifier(newNoms)).size).isEqualTo(1)

    publishEventToTopic(mergeIdentifiers(oldNoms, newNoms))

    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    assertThat(noteRepository.findAll(matchesPersonIdentifier(newNoms)).size).isEqualTo(3)
    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(0)
  }

  @Test
  fun `receiving a merge event without case notes does not cause a failure`() {
    val oldNoms = personIdentifier()
    val newNoms = personIdentifier()
    publishEventToTopic(mergeIdentifiers(oldNoms, newNoms))
    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  private fun mergeIdentifiers(oldNoms: String, newNoms: String) = DomainEvent(
    ZonedDateTime.now(),
    PRISONER_MERGED,
    null,
    "A prisoner has been merged",
    MergeInformation(newNoms, oldNoms),
    PersonReference.withIdentifier(newNoms),
  )
}
