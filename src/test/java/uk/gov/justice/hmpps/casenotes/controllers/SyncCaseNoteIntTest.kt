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
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent
import uk.gov.justice.hmpps.casenotes.notes.DeletedCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.sync.Author
import uk.gov.justice.hmpps.casenotes.sync.SyncCaseNoteAmendmentRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.sync.SyncResult
import uk.gov.justice.hmpps.casenotes.sync.SyncResult.Action.CREATED
import uk.gov.justice.hmpps.casenotes.sync.SyncResult.Action.UPDATED
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.LocalDateTime
import java.util.UUID

class SyncCaseNoteIntTest : ResourceTest() {

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
    saved.verifyAgainst(request)

    hmppsEventsQueue.receiveDomainEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.NOMIS, saved)
  }

  @Test
  fun `201 created - sync creates a new case note with amendments`() {
    val request = syncCaseNoteRequest(amendments = setOf(syncAmendmentRequest()))
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.CREATED)
    assertThat(response.action).isEqualTo(CREATED)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(response.id, request.personIdentifier))
    saved.verifyAgainst(request)
    val amended = saved.amendments().first()
    amended.verifyAgainst(request.amendments.first())

    hmppsEventsQueue.receiveDomainEvent().verifyAgainst(PersonCaseNoteEvent.Type.CREATED, Source.NOMIS, saved)
  }

  @Test
  fun `200 ok - sync updates an existing case note using id`() {
    val prisonNumber = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(prisonNumber))
    val request = existing.syncRequest()
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.OK)
    assertThat(response.action).isEqualTo(UPDATED)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(response.id, request.personIdentifier))
    saved.verifyAgainst(request)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(UPDATE)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receiveDomainEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.NOMIS, saved)
  }

  @Test
  fun `200 ok - sync updates an existing case note using legacy id`() {
    val prisonNumber = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(prisonNumber))
    val request = existing.syncRequest().copy(id = null)
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.OK)
    assertThat(response.action).isEqualTo(UPDATED)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(response.id, request.personIdentifier))
    saved.verifyAgainst(request)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(UPDATE)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receiveDomainEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.NOMIS, saved)
  }

  @Test
  fun `200 ok - sync updates an existing case note with amendments`() {
    val prisonNumber = personIdentifier()
    val existing = givenCaseNote(
      generateCaseNote(prisonNumber).withAmendment(createdAt = LocalDateTime.now().minusSeconds(5)),
    )
    val request = existing.syncRequest().let { r ->
      r.copy(
        amendments = (
          r.amendments + syncAmendmentRequest(
            "A new amendment",
            createdDateTime = LocalDateTime.now().minusSeconds(10),
          )
          ).toSortedSet(compareBy { it.createdDateTime }),
      )
    }
    val response = syncCaseNote(request).success<SyncResult>(HttpStatus.OK)
    assertThat(response.action).isEqualTo(UPDATED)
    assertThat(response.id).isEqualTo(existing.id)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(existing.id, request.personIdentifier))
    saved.verifyAgainst(request)
    assertThat(saved.amendments().size).isEqualTo(2)
    val amend = saved.amendments().first()
    amend.verifyAgainst(request.amendments.first())

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(UPDATE)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receiveDomainEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.NOMIS, saved)
  }

  @Test
  fun `204 no content - delete case note`() {
    val prisonNumber = personIdentifier()
    val note = givenCaseNote(generateCaseNote(prisonNumber))
    val cns = { noteRepository.findByIdOrNull(note.id) }
    assertThat(cns()).isNotNull()

    deleteCaseNote(note.id)

    assertThat(cns()).isNull()

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(note.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(DELETE)
    deleted.caseNote.verifyAgainst(note)

    hmppsEventsQueue.receiveDomainEvent().verifyAgainst(PersonCaseNoteEvent.Type.DELETED, Source.NOMIS, note)
  }

  @Test
  fun `204 no content - delete non existent case note`() {
    deleteCaseNote(UUID.randomUUID())
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
  occurrenceDateTime: LocalDateTime = LocalDateTime.now().minusDays(2),
  text: String = "The text of the case note",
  systemGenerated: Boolean = false,
  author: Author = defaultAuthor(),
  createdDateTime: LocalDateTime = LocalDateTime.now().minusDays(1),
  createdBy: String = "CreatedByUsername",
  source: Source = Source.NOMIS,
  amendments: Set<SyncCaseNoteAmendmentRequest> = setOf(),
) = SyncCaseNoteRequest(
  legacyId,
  id,
  prisonIdentifier,
  locationId,
  type,
  subType,
  occurrenceDateTime,
  text,
  systemGenerated,
  author,
  createdDateTime,
  createdBy,
  source,
  amendments,
)

private fun syncAmendmentRequest(
  text: String = "The text of the case note",
  author: Author = defaultAuthor(),
  createdDateTime: LocalDateTime = LocalDateTime.now(),
) = SyncCaseNoteAmendmentRequest(text, author, createdDateTime)

private fun defaultAuthor() = Author("AuthorUsername", "12376471", "Author", "Name")

private fun Note.syncRequest(): SyncCaseNoteRequest {
  val authorNames = authorName.split(" ")
  return syncCaseNoteRequest(
    legacyId = legacyId,
    id = id,
    locationId = locationId,
    prisonIdentifier = personIdentifier,
    text = "The text has been updated from nomis",
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
    text = "An updated amendment of the case note",
    author = Author(authorUsername, authorUserId, authorNames[0], authorNames[1]),
    createdDateTime = createdAt,
  )
}
