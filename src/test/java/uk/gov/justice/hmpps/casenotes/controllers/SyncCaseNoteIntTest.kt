package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_SYNC
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.Amendment
import uk.gov.justice.hmpps.casenotes.domain.DeletionCause.DELETE
import uk.gov.justice.hmpps.casenotes.domain.DeletionCause.UPDATE
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.System
import uk.gov.justice.hmpps.casenotes.domain.audit.DeletedCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.sync.Author
import uk.gov.justice.hmpps.casenotes.sync.CreatedBetween
import uk.gov.justice.hmpps.casenotes.sync.ResendPersonCaseNoteEvents
import uk.gov.justice.hmpps.casenotes.sync.SyncCaseNoteAmendmentRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncResult
import uk.gov.justice.hmpps.casenotes.sync.SyncResult.Action.CREATED
import uk.gov.justice.hmpps.casenotes.sync.SyncResult.Action.UPDATED
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit
import java.util.UUID

class SyncCaseNoteIntTest : IntegrationTest() {

  @Autowired
  lateinit var deletedCaseNoteRepository: DeletedCaseNoteRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.put().uri(BASE_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    syncCaseNote(syncCaseNoteRequest(), roles = listOf(ROLE_CASE_NOTES_WRITE)).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - field validation failures`() {
    val request = syncCaseNoteRequest(
      prisonIdentifier = "n".repeat(13),
      type = "n".repeat(13),
      subType = "n".repeat(13),
      locationId = "n".repeat(13),
      text = "",
      author = Author("n".repeat(65), "n".repeat(65), "Something", "Else"),
      amendments = setOf(
        syncAmendmentRequest(
          author = Author("", "", "", ""),
          text = "",
        ),
      ),
    )
    val response = syncCaseNote(request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo(
        """
        |400 BAD_REQUEST Validation failures: 
        |author first name cannot be blank
        |author last name cannot be blank
        |author user id cannot be blank
        |author user id cannot be more than 64 characters
        |author username cannot be blank
        |author username cannot be more than 64 characters
        |location must be no more than 12 characters
        |person identifier cannot be more than 12 characters
        |sub type must be no more than 12 characters
        |type must be no more than 12 characters
        |
        """.trimMargin(),
      )
    }
  }

  @Test
  fun `400 bad request - exception thrown if person identifier doesn't match`() {
    val existing = givenCaseNote(generateCaseNote())
    val request = existing.syncRequest().copy(personIdentifier = personIdentifier())
    val response = syncCaseNote(request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(response.developerMessage)
      .isEqualTo("Case note belongs to another prisoner or prisoner records have been merged")
  }

  @Test
  fun `201 created - sync creates a new case note`() {
    val request = syncCaseNoteRequest()
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.CREATED)
    assertThat(response.action).isEqualTo(CREATED)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(response.id, request.personIdentifier))
    assertThat(response.legacyId).isEqualTo(request.legacyId)
    saved.verifyAgainst(request)
    assertThat(saved.system).isEqualTo(System.NOMIS)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.NOMIS, saved)
  }

  @Test
  fun `201 created - sync creates a new case note with amendments`() {
    val request = syncCaseNoteRequest(amendments = setOf(syncAmendmentRequest()))
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.CREATED)
    assertThat(response.action).isEqualTo(CREATED)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(response.id, request.personIdentifier))
    assertThat(response.legacyId).isEqualTo(request.legacyId)
    saved.verifyAgainst(request)
    assertThat(saved.system).isEqualTo(System.NOMIS)
    val amended = saved.amendments().first()
    amended.verifyAgainst(request.amendments.first())
    assertThat(amended.system).isEqualTo(System.NOMIS)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.NOMIS, saved)
  }

  @Test
  fun `200 ok - sync updates an existing case note using id retaining dps legacy id`() {
    val personIdentifier = personIdentifier()
    val dpsLegacyId = noteRepository.getNextLegacyId()
    val existing = givenCaseNote(generateCaseNote(personIdentifier, legacyId = dpsLegacyId))
    val request =
      existing.syncRequest()
        .copy(text = "The text was updated in nomis by CENTRAL ADMIN", system = System.DPS, legacyId = 12)
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.OK)
    assertThat(response.action).isEqualTo(UPDATED)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(response.id, request.personIdentifier))
    assertThat(response.legacyId).isEqualTo(request.legacyId)
    saved.verifyAgainst(request, dpsLegacyId)
    assertThat(saved.system).isEqualTo(System.DPS)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(UPDATE)
    assertThat(deleted.system).isEqualTo(System.NOMIS)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.NOMIS, saved)
  }

  @Test
  fun `200 ok - sync updates case note occurred at`() {
    val personIdentifier = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(personIdentifier))
    val request =
      existing.syncRequest().copy(occurrenceDateTime = existing.occurredAt.minusHours(4), system = System.DPS)
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.OK)
    assertThat(response.action).isEqualTo(UPDATED)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(response.id, request.personIdentifier))
    saved.verifyAgainst(request)
    assertThat(saved.system).isEqualTo(System.DPS)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(UPDATE)
    assertThat(deleted.system).isEqualTo(System.NOMIS)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.NOMIS, saved)
  }

  @Test
  fun `200 ok - sync updates an existing case note type`() {
    val types = getAllTypes()
    val type1 = types.random()
    val type2 = types.filter { it.typeCode != type1.typeCode && it.code != type1.code }.random()
    val personIdentifier = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(personIdentifier, type1))
    val request = existing.syncRequest().copy(type = type2.typeCode, subType = type2.code, system = System.DPS)
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.OK)
    assertThat(response.action).isEqualTo(UPDATED)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(response.id, request.personIdentifier))
    saved.verifyAgainst(request)
    assertThat(saved.system).isEqualTo(System.DPS)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(UPDATE)
    assertThat(deleted.system).isEqualTo(System.NOMIS)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.NOMIS, saved)
  }

  @Test
  fun `200 ok - sync amends a case note`() {
    val personIdentifier = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(personIdentifier).withAmendment(createdAt = now().minusDays(5)))
    val newAmendment = syncAmendmentRequest("A new amendment", createdDateTime = now().minusDays(2))
    val request = existing.syncRequest().let { it.copy(amendments = it.amendments + newAmendment) }
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.OK)
    assertThat(response.action).isEqualTo(UPDATED)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(response.id, request.personIdentifier))
    saved.verifyAgainst(request)
    assertThat(saved.system).isEqualTo(System.DPS)
    assertThat(saved.amendments()).hasSize(2)
    assertThat(saved.amendments().map { it.system }).containsExactlyInAnyOrder(System.DPS, System.NOMIS)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted).isNull()

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.NOMIS, saved)
  }

  @Test
  fun `200 ok - sync updates an existing case note using legacy id`() {
    val personIdentifier = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(personIdentifier, system = System.NOMIS))
    val request = existing.syncRequest().copy(id = null, text = "The text was updated in nomis by CENTRAL ADMIN")
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.OK)
    assertThat(response.action).isEqualTo(UPDATED)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(response.id, request.personIdentifier))
    saved.verifyAgainst(request)
    assertThat(saved.system).isEqualTo(System.NOMIS)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(UPDATE)
    assertThat(deleted.system).isEqualTo(System.NOMIS)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.NOMIS, saved)
  }

  @Test
  fun `200 ok - sync updates an existing case note with amendments`() {
    val personIdentifier = personIdentifier()
    val existing = givenCaseNote(
      generateCaseNote(personIdentifier).withAmendment(createdAt = now().minusSeconds(5)),
    )
    val request = existing.syncRequest().let { r ->
      r.copy(
        system = System.NOMIS,
        amendments = (
          r.amendments.map { it.copy(text = "The text was updated in nomis by CENTRAL ADMIN") } +
            syncAmendmentRequest("A new amendment", createdDateTime = now().minusSeconds(10))
          ).toSortedSet(compareBy { it.createdDateTime }),
      )
    }
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.OK)
    assertThat(response.action).isEqualTo(UPDATED)
    assertThat(response.id).isEqualTo(existing.id)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(existing.id, request.personIdentifier))
    saved.verifyAgainst(request)
    assertThat(saved.system).isEqualTo(System.NOMIS)
    assertThat(saved.amendments().size).isEqualTo(2)
    val amend = saved.amendments().first()
    amend.verifyAgainst(request.amendments.first())
    assertThat(amend.system).isEqualTo(System.NOMIS)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(UPDATE)
    assertThat(deleted.system).isEqualTo(System.NOMIS)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.NOMIS, saved)
  }

  @Test
  fun `204 no content - delete case note`() {
    val personIdentifier = personIdentifier()
    val note = givenCaseNote(generateCaseNote(personIdentifier))
    val cns = { noteRepository.findByIdOrNull(note.id) }
    assertThat(cns()).isNotNull()

    deleteCaseNote(note.id)

    assertThat(cns()).isNull()

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(note.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(DELETE)
    assertThat(deleted.system).isEqualTo(System.NOMIS)
    deleted.caseNote.verifyAgainst(note)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.DELETED, Source.NOMIS, note)
  }

  @Test
  fun `204 no content - delete non existent case note`() {
    deleteCaseNote(UUID.randomUUID())
  }

  @Test
  fun `200 ok - can retrieve all sync to nomis case notes`() {
    val personIdentifier = personIdentifier()
    val cns = (1..100).map { i ->
      val cn = generateCaseNote(personIdentifier)
      if (i % 2 == 0) cn.withAmendment()
      givenCaseNote(cn)
    }

    val expected = cns.filter { it.subType.syncToNomis }
    val caseNotes = getCaseNotes(personIdentifier)
    assertThat(caseNotes).hasSize(expected.size)
    assertThat(caseNotes.flatMap(CaseNote::amendments)).hasSize(expected.flatMap { it.amendments() }.size)
  }

  @Test
  fun `200 ok - resends case note updated event`() {
    val personIdentifier = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(personIdentifier).withAmendment(createdAt = now().minusDays(5)))

    val request = ResendPersonCaseNoteEvents(setOf(existing.id))
    webTestClient.post().uri("/resend-person-case-note-events")
      .bodyValue(request)
      .exchange().expectStatus().isNoContent

    val saved = requireNotNull(noteRepository.findByIdOrNull(existing.id))
    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.DPS, saved)
  }

  @Test
  fun `200 ok - resends case note respects include sync to nomis limiter`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes()
    val stn = givenCaseNote(
      generateCaseNote(personIdentifier, types.filter { it.syncToNomis }.random(), createdAt = now().minusDays(50)),
    )
    val nstn = givenCaseNote(
      generateCaseNote(personIdentifier, types.filter { !it.syncToNomis }.random(), createdAt = now().minusDays(50)),
    )

    val request1 = ResendPersonCaseNoteEvents(setOf(), CreatedBetween(now().minusDays(51), now().minusDays(49)))
    webTestClient.post().uri("/resend-person-case-note-events")
      .bodyValue(request1)
      .exchange().expectStatus().isNoContent

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.DPS, nstn)

    val request2 = ResendPersonCaseNoteEvents(
      setOf(),
      CreatedBetween(now().minusDays(51), now().minusDays(49), includeSyncToNomis = true),
    )
    webTestClient.post().uri("/resend-person-case-note-events")
      .bodyValue(request2)
      .exchange().expectStatus().isNoContent

    val events = hmppsEventsQueue.receivePersonCaseNoteEventsOnQueue()
    assertThat(events).hasSize(2)
    events.forEach { e ->
      val caseNote = listOf(stn, nstn).first { it.id == e.additionalInformation.id }
      val eventType = if (caseNote.amendments().isEmpty()) {
        PersonCaseNoteEvent.Type.CREATED
      } else {
        PersonCaseNoteEvent.Type.UPDATED
      }
      e.verifyAgainst(eventType, Source.DPS, caseNote)
    }
  }

  @Test
  fun `200 ok - resends case note created and updated event for date range`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes().filter { !it.syncToNomis }
    val toFind = listOf(
      givenCaseNote(generateCaseNote(personIdentifier, types.random(), createdAt = now().minusDays(80))),
      givenCaseNote(
        generateCaseNote(
          personIdentifier,
          types.random(),
          createdAt = now().minusDays(120),
        ).withAmendment(createdAt = now().minusDays(80)),
      ),
    )
    givenCaseNote(generateCaseNote(personIdentifier, types.random(), createdAt = now().minusDays(70)))
    givenCaseNote(generateCaseNote(personIdentifier, types.random(), createdAt = now().minusDays(90)))
    givenCaseNote(
      generateCaseNote(personIdentifier, types.random(), createdAt = now().minusDays(120))
        .withAmendment(createdAt = now()),
    )
    givenCaseNote(
      generateCaseNote(personIdentifier, types.random(), createdAt = now().minusDays(90))
        .withAmendment(createdAt = now().minusDays(90)),
    )

    val request = ResendPersonCaseNoteEvents(setOf(), CreatedBetween(now().minusDays(85), now().minusDays(75)))
    webTestClient.post().uri("/resend-person-case-note-events")
      .bodyValue(request)
      .exchange().expectStatus().isNoContent

    val saved = noteRepository.findAllByIdIn(toFind.map { it.id })
    val events = hmppsEventsQueue.receivePersonCaseNoteEventsOnQueue()
    assertThat(events).hasSize(2)
    events.forEach { e ->
      val caseNote = saved.first { it.id == e.additionalInformation.id }
      val eventType = if (caseNote.amendments().isEmpty()) {
        PersonCaseNoteEvent.Type.CREATED
      } else {
        PersonCaseNoteEvent.Type.UPDATED
      }
      e.verifyAgainst(eventType, Source.DPS, caseNote)
    }
  }

  private fun syncCaseNote(
    request: SyncCaseNoteRequest,
    roles: List<String> = listOf(ROLE_CASE_NOTES_SYNC),
    tokenUsername: String = USERNAME,
  ) = webTestClient.put().uri(BASE_URL).headers(addBearerAuthorisation(tokenUsername, roles))
    .bodyValue(request).exchange()

  private fun deleteCaseNote(
    id: UUID,
    roles: List<String> = listOf(ROLE_CASE_NOTES_SYNC),
    tokenUsername: String = USERNAME,
  ) = webTestClient.delete().uri("$BASE_URL/$id")
    .headers(addBearerAuthorisation(tokenUsername, roles))
    .exchange().expectStatus().isNoContent

  private fun getCaseNotes(
    personIdentifier: String,
    roles: List<String> = listOf(ROLE_CASE_NOTES_SYNC),
    tokenUsername: String = USERNAME,
  ) = webTestClient.get().uri("$BASE_URL/$personIdentifier")
    .headers(addBearerAuthorisation(tokenUsername, roles))
    .exchange().successList<CaseNote>()

  companion object {
    private const val BASE_URL = "/sync/case-notes"
  }
}

private fun syncCaseNoteRequest(
  legacyId: Long = NomisIdGenerator.newId(),
  id: UUID? = null,
  prisonIdentifier: String = personIdentifier(),
  locationId: String = "SWI",
  type: String = "OMIC",
  subType: String = "GEN",
  occurrenceDateTime: LocalDateTime = now().minusDays(2),
  text: String = "The text of the case note",
  systemGenerated: Boolean = false,
  author: Author = defaultAuthor(),
  createdDateTime: LocalDateTime = now().minusDays(1),
  createdBy: String = "CreatedByUsername",
  amendments: Set<SyncCaseNoteAmendmentRequest> = setOf(),
  system: System? = null,
) = SyncCaseNoteRequest(
  legacyId,
  id,
  prisonIdentifier,
  locationId,
  type,
  subType,
  occurrenceDateTime.truncatedTo(ChronoUnit.SECONDS),
  text,
  systemGenerated,
  author,
  createdDateTime,
  createdBy,
  amendments,
  system,
)

private fun syncAmendmentRequest(
  text: String = "The text of the case note",
  author: Author = defaultAuthor(),
  createdDateTime: LocalDateTime = now(),
  system: System? = null,
) = SyncCaseNoteAmendmentRequest(text, author, createdDateTime, system)

private fun defaultAuthor() = Author("AuthorUsername", "12376471", "Author", "Name")

private fun Note.syncRequest(): SyncCaseNoteRequest {
  val authorNames = authorName.split(" ")
  return syncCaseNoteRequest(
    type = subType.typeCode,
    subType = subType.code,
    legacyId = legacyId,
    id = id,
    locationId = locationId,
    prisonIdentifier = personIdentifier,
    text = text,
    createdDateTime = createdAt,
    occurrenceDateTime = occurredAt,
    author = Author(authorUsername, authorUserId, authorNames[0], authorNames[1]),
    createdBy = createdBy,
    amendments = amendments().map { it.syncRequest() }.toSortedSet(compareBy { it.createdDateTime }),
  )
}

private fun Amendment.syncRequest(): SyncCaseNoteAmendmentRequest {
  val authorNames = authorName.split(" ")
  return syncAmendmentRequest(
    text = text,
    author = Author(authorUsername, authorUserId, authorNames[0], authorNames[1]),
    createdDateTime = createdAt,
  )
}
