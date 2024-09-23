package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_SYNC
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.matchesPrisonNumber
import uk.gov.justice.hmpps.casenotes.sync.Author
import uk.gov.justice.hmpps.casenotes.sync.MigrateAmendmentRequest
import uk.gov.justice.hmpps.casenotes.sync.MigrateCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.sync.MigrationResult
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.Duration
import java.time.LocalDateTime
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

class MigrateCaseNotesIntTest : ResourceTest() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(SYNC_URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    migrateCaseNotes(listOf(migrateCaseNoteRequest()), listOf(ROLE_CASE_NOTES_WRITE)).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - case note types must exist`() {
    val request = listOf(
      migrateCaseNoteRequest(type = "NON_EXISTENT", subType = "GARBAGE"),
      migrateCaseNoteRequest(type = "NON_EXISTENT", subType = "NOT_THERE"),
      migrateCaseNoteRequest(type = "ANOTHER_TYPE", subType = "GARBAGE"),
    )
    val response = migrateCaseNotes(request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Case note types missing: { ANOTHER_TYPE:[GARBAGE], NON_EXISTENT:[GARBAGE, NOT_THERE] }")
    }
  }

  @Test
  fun `400 bad request - case note types be sync to nomis types`() {
    val request = listOf(
      migrateCaseNoteRequest(type = "POM", subType = "SPECIAL"),
      migrateCaseNoteRequest(type = "POM", subType = "GEN"),
      migrateCaseNoteRequest(type = "OMIC", subType = "GEN"),
      migrateCaseNoteRequest(type = "OMIC", subType = "OMIC_OPEN"),
    )
    val response = migrateCaseNotes(request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo("Case note types are not sync to nomis types: { OMIC:[GEN, OMIC_OPEN], POM:[GEN, SPECIAL] }")
    }
  }

  @Test
  fun `200 ok - new case notes created`() {
    val personIdentifier = personIdentifier()
    val types = getAllTypes().filter { it.syncToNomis }
    val request = (0..500).map {
      val type = types.random()
      migrateCaseNoteRequest(
        prisonIdentifier = personIdentifier,
        type = type.type.code,
        subType = type.code,
        amendments = if (it % 5 == 0) setOf(migrateAmendmentRequest(), migrateAmendmentRequest()) else setOf(),
      )
    }
    val (response, elapsed) = measureTimedValue { migrateCaseNotes(request).successList<MigrationResult>() }
    assertThat(response.size).isEqualTo(request.size)
    assertThat(elapsed.toJavaDuration()).isLessThanOrEqualTo(Duration.ofSeconds(2))
  }

  @Test
  fun `200 ok - new case note with amendments is correctly stored`() {
    val type = getAllTypes().first { it.syncToNomis }
    val amendment = migrateAmendmentRequest(
      text = "An amendment to the case note, to verify it is saved correctly",
      author = Author("SM1ELSE", "585153477", "Simon", "Else"),
      createdDateTime = LocalDateTime.now().minusDays(3),
    )
    val request = migrateCaseNoteRequest(
      prisonIdentifier = personIdentifier(),
      locationId = "LEI",
      type = type.type.code,
      subType = type.code,
      text = "This a larger, non default text block to determine that notes are correctly saved to the db",
      author = Author("anotherUser", "564716341", "An", "Other"),
      occurrenceDateTime = LocalDateTime.now().minusDays(10),
      createdDateTime = LocalDateTime.now().minusDays(7),
      amendments = setOf(amendment),
    )

    val response = migrateCaseNotes(listOf(request)).successList<MigrationResult>()
    val saved = noteRepository.findByIdAndPersonIdentifier(response.first().id, request.personIdentifier)
    requireNotNull(saved).verifyAgainst(request)
    saved.amendments().first().verifyAgainst(request.amendments.first())
  }

  @Test
  fun `200 ok - some case note already exists`() {
    val prisonNumber = personIdentifier()
    val types = getAllTypes().filter { it.syncToNomis }
    val migrated = givenCaseNote(generateCaseNote(prisonNumber, types.random()).withAmendment().withAmendment())
    val secondType = types.random()
    val request = listOf(
      migrateCaseNoteRequest(prisonIdentifier = prisonNumber, type = secondType.typeCode, subType = secondType.code),
      migrated.migrateRequest(),
    )

    val response = migrateCaseNotes(request).successList<MigrationResult>()
    assertThat(response.size).isEqualTo(request.size)
    assertThat(response.map { it.legacyId }).containsExactlyInAnyOrderElementsOf(request.map { it.legacyId })

    val saved = noteRepository.findAll(matchesPrisonNumber(prisonNumber))
    assertThat(saved.size).isEqualTo(request.size)
  }

  private fun migrateCaseNotes(
    request: List<MigrateCaseNoteRequest>,
    roles: List<String> = listOf(ROLE_CASE_NOTES_SYNC),
    username: String = "NOMIS_TO_DPS",
  ) = webTestClient.post().uri(SYNC_URL)
    .headers(addBearerAuthorisation(username, roles))
    .bodyValue(request)
    .exchange()

  companion object {
    private const val SYNC_URL = "/migrate/case-notes"
  }
}

private fun migrateCaseNoteRequest(
  legacyId: Long = NomisIdGenerator.newId(),
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
  amendments: Set<MigrateAmendmentRequest> = setOf(),
) = MigrateCaseNoteRequest(
  legacyId,
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

private fun migrateAmendmentRequest(
  text: String = "The text of the case note",
  author: Author = defaultAuthor(),
  createdDateTime: LocalDateTime = LocalDateTime.now(),
) = MigrateAmendmentRequest(text, author, createdDateTime)

private fun defaultAuthor() = Author("AuthorUsername", "12376471", "Author", "Name")

private fun Note.migrateRequest(): MigrateCaseNoteRequest {
  val authorNames = authorName.split(" ")
  return migrateCaseNoteRequest(
    legacyId = legacyId,
    locationId = locationId,
    prisonIdentifier = personIdentifier,
    text = text,
    createdDateTime = createdAt,
    occurrenceDateTime = occurredAt,
    author = Author(authorUsername, authorUserId, authorNames[0], authorNames[1]),
    createdBy = createdBy,
    type = subType.type.code,
    subType = subType.code,
  )
}
