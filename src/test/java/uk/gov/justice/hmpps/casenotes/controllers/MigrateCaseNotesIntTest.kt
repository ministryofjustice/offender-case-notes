package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_SYNC
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.System
import uk.gov.justice.hmpps.casenotes.domain.matchesPersonIdentifier
import uk.gov.justice.hmpps.casenotes.sync.Author
import uk.gov.justice.hmpps.casenotes.sync.MigrateAmendmentRequest
import uk.gov.justice.hmpps.casenotes.sync.MigrateCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.sync.MigrationResult
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import java.time.LocalDateTime

class MigrateCaseNotesIntTest : IntegrationTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(personIdentifier())).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    migrateCaseNotes(personIdentifier(), listOf(migrateCaseNoteRequest()), listOf(ROLE_CASE_NOTES_WRITE))
      .expectStatus().isForbidden
  }

  @Test
  fun `200 ok - replace existing nomis notes and insert new ones`() {
    val personIdentifier = personIdentifier()
    val (nomisTypes, dpsTypes) = getAllTypes().partition { it.syncToNomis }
    val migrated = (0..20).map {
      val caseNote = generateCaseNote(personIdentifier, nomisTypes.random())
      if (it % 2 == 0) {
        caseNote.withAmendment().withAmendment()
      }
      givenCaseNote(caseNote)
    }

    val duplicate = givenCaseNote(migrated.random().duplicate())
    val dpsNotes = (0..5).map {
      givenCaseNote(generateCaseNote(personIdentifier, dpsTypes.random(), legacyId = noteRepository.getNextLegacyId()))
    }

    val extraType = nomisTypes.random()
    val extra = migrateCaseNoteRequest(type = extraType.typeCode, subType = extraType.code)
    val request = migrated.map(Note::migrateRequest) + extra
    val response = migrateCaseNotes(personIdentifier, request).successList<MigrationResult>()
    assertThat(response.size).isEqualTo(request.size)
    assertThat(response.map { it.legacyId }).containsExactlyInAnyOrderElementsOf(request.map { it.legacyId })

    val saved = noteRepository.findAll(matchesPersonIdentifier(personIdentifier))
    assertThat(saved.size).isEqualTo(request.size + dpsNotes.size)
    assertThat(saved.none { it.id == duplicate.id })
    assertThat(saved.firstOrNull { it.legacyId == extra.legacyId }).isNotNull
  }

  private fun migrateCaseNotes(
    personIdentifier: String,
    request: List<MigrateCaseNoteRequest>,
    roles: List<String> = listOf(ROLE_CASE_NOTES_SYNC),
    username: String = "NOMIS_TO_DPS",
  ) = webTestClient.post().uri(urlToTest(personIdentifier))
    .headers(addBearerAuthorisation(username, roles))
    .bodyValue(request)
    .exchange()

  companion object {
    private fun urlToTest(personIdentifier: String) = "/migrate/case-notes/$personIdentifier"
  }
}

private fun migrateCaseNoteRequest(
  legacyId: Long = NomisIdGenerator.newId(),
  locationId: String = "SWI",
  type: String = "OMIC",
  subType: String = "GEN",
  occurrenceDateTime: LocalDateTime = LocalDateTime.now().minusDays(2),
  text: String = "The text of the case note",
  systemGenerated: Boolean = false,
  author: Author = defaultAuthor(),
  createdDateTime: LocalDateTime = LocalDateTime.now().minusDays(1),
  createdBy: String = "CreatedByUsername",
  system: System = System.NOMIS,
  amendments: Set<MigrateAmendmentRequest> = setOf(),
) = MigrateCaseNoteRequest(
  legacyId,
  locationId,
  type,
  subType,
  occurrenceDateTime,
  text,
  systemGenerated,
  system,
  author,
  createdDateTime,
  createdBy,
  amendments,
)

private fun defaultAuthor() = Author("AuthorUsername", "12376471", "Author", "Name")

private fun Note.migrateRequest(): MigrateCaseNoteRequest {
  val authorNames = authorName.split(" ")
  return migrateCaseNoteRequest(
    legacyId = legacyId,
    locationId = locationId,
    text = text,
    createdDateTime = createdAt,
    occurrenceDateTime = occurredAt,
    author = Author(authorUsername, authorUserId, authorNames[0], authorNames[1]),
    createdBy = createdBy,
    type = subType.type.code,
    subType = subType.code,
  )
}

private fun Note.duplicate() = Note(
  personIdentifier,
  subType,
  occurredAt,
  locationId,
  authorUsername,
  authorUserId,
  authorName,
  text,
  systemGenerated,
  system,
).apply { legacyId = NomisIdGenerator.newId() }
