package uk.gov.justice.hmpps.casenotes.services

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest
import uk.gov.justice.hmpps.casenotes.domain.DeletionCause
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.audit.DeletedCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.domain.matchesPersonIdentifier
import uk.gov.justice.hmpps.casenotes.events.DomainEvent
import uk.gov.justice.hmpps.casenotes.events.DomainEventListener.Companion.PRISONER_MERGED
import uk.gov.justice.hmpps.casenotes.events.MergeInformation
import uk.gov.justice.hmpps.casenotes.events.PersonReference
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.ZonedDateTime
import java.util.UUID

class MergeEventIntTest : IntegrationTest() {
  @Autowired
  lateinit var deletedRepository: DeletedCaseNoteRepository

  @Test
  fun `receiving a merge event updates person identifier`() {
    val oldNoms = personIdentifier()
    val cn1 = givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    val cn2 = givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(2)

    val newNoms = personIdentifier()
    publishEventToTopic(mergeIdentifiers(oldNoms, newNoms))

    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    assertThat(noteRepository.findAll(matchesPersonIdentifier(newNoms)).size).isEqualTo(2)
    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(0)

    verifyMergeAudit(cn1)
    verifyMergeAudit(cn2)

    verifyEvents(oldNoms, newNoms, cn1.id, cn2.id)
  }

  @Test
  fun `receiving a merge event merges all existing case notes`() {
    val oldNoms = personIdentifier()
    val cn1 = givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    val cn2 = givenCaseNote(generateCaseNote(oldNoms).withAmendment().withAmendment())
    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(2)

    val newNoms = personIdentifier()
    givenCaseNote(generateCaseNote(newNoms).withAmendment().withAmendment())
    assertThat(noteRepository.findAll(matchesPersonIdentifier(newNoms)).size).isEqualTo(1)

    publishEventToTopic(mergeIdentifiers(oldNoms, newNoms))

    await untilCallTo { domainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    assertThat(noteRepository.findAll(matchesPersonIdentifier(newNoms)).size).isEqualTo(3)
    assertThat(noteRepository.findAll(matchesPersonIdentifier(oldNoms)).size).isEqualTo(0)

    verifyMergeAudit(cn1)
    verifyMergeAudit(cn2)

    verifyEvents(oldNoms, newNoms, cn1.id, cn2.id)
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

  private fun verifyMergeAudit(note: Note) {
    val deleted = deletedRepository.findByCaseNoteId(note.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(DeletionCause.MERGE)
    assertThat(deleted.deletedBy).isEqualTo("SYS")
    deleted.caseNote.verifyAgainst(note)
  }

  private fun verifyEvents(oldNoms: String, newNoms: String, vararg ids: UUID) {
    val events = hmppsEventsQueue.receivePersonCaseNoteEventsOnQueue()
    assertThat(events).hasSize(ids.size)
    val personIdentifiers = events.map { it.personReference.findNomsNumber() }.toSet()
    assertThat(personIdentifiers).hasSize(1)
    assertThat(personIdentifiers.first()).isEqualTo(newNoms)
    assertThat(events.map { it.additionalInformation.previousNomsNumber to it.additionalInformation.id })
      .containsExactlyInAnyOrderElementsOf(ids.map { oldNoms to it })
  }
}
