package uk.gov.justice.hmpps.casenotes.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType
import java.time.LocalDateTime

@ActiveProfiles(profiles = ["test"])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class OffenderCaseNoteRepositoryTest {

  @Autowired
  private lateinit var repository: OffenderCaseNoteRepository

  @Autowired
  private lateinit var caseNoteTypeRepository: CaseNoteTypeRepository

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var genType: SensitiveCaseNoteType

  @BeforeEach
  fun setUp() {
    val jwt = Jwt.withTokenValue("some").subject("anonymous").header("head", "something").build()
    SecurityContextHolder.getContext().authentication = AuthAwareAuthenticationToken(jwt, "userId", emptyList())
    genType = caseNoteTypeRepository.findSensitiveCaseNoteTypeByParentType_TypeAndType(PARENT_TYPE, SUB_TYPE)!!
  }

  @Test
  fun testPersistCaseNote() {
    val caseNote = transientEntity(OFFENDER_IDENTIFIER)
    val persistedEntity = repository.save(caseNote)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.id).isNotNull
    TestTransaction.start()
    val retrievedEntity = repository.findById(persistedEntity.id).orElseThrow()

    // equals only compares the business key columns
    assertThat(retrievedEntity).isEqualTo(caseNote)
    assertThat(retrievedEntity.createUserId).isEqualTo("anonymous")
  }

  @Test
  @WithAnonymousUser
  fun testPersistCaseNoteAndAmendment() {
    val caseNote = transientEntity(OFFENDER_IDENTIFIER)
    caseNote.addAmendment("Another Note 0", "someuser", "Some User", "user id")
    assertThat(caseNote.amendments).hasSize(1)
    val persistedEntity = repository.save(caseNote)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.id).isNotNull
    TestTransaction.start()
    val retrievedEntity = repository.findById(persistedEntity.id).orElseThrow()
    retrievedEntity.addAmendment("Another Note 1", "someuser", "Some User", "user id")
    retrievedEntity.addAmendment("Another Note 2", "someuser", "Some User", "user id")
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity2 = repository.findById(persistedEntity.id).orElseThrow()
    assertThat(retrievedEntity2.amendments).hasSize(3)
    assertThat(retrievedEntity2.getAmendment(1).orElseThrow().noteText).isEqualTo("Another Note 0")
    val offenderCaseNoteAmendment3 = retrievedEntity2.getAmendment(3).orElseThrow()
    assertThat(offenderCaseNoteAmendment3.noteText).isEqualTo("Another Note 2")
    retrievedEntity2.addAmendment("Another Note 3", "USER1", "Mickey Mouse", "user id")
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity3 = repository.findById(persistedEntity.id).orElseThrow()
    assertThat(retrievedEntity3.amendments).hasSize(4)
    assertThat(retrievedEntity3.getAmendment(4).orElseThrow().noteText).isEqualTo("Another Note 3")
  }

  @Test
  fun testOffenderCaseNoteFilter() {
    val entity = OffenderCaseNote.builder()
        .occurrenceDateTime(LocalDateTime.now())
        .locationId("BOB")
        .authorUsername("FILTER")
        .authorUserId("some id")
        .authorName("Mickey Mouse")
        .offenderIdentifier(OFFENDER_IDENTIFIER)
        .sensitiveCaseNoteType(genType)
        .noteText("HELLO")
        .build()
    repository.save(entity)
    val allCaseNotes = repository.findAll(OffenderCaseNoteFilter.builder()
        .type(" ").subType(" ").authorUsername(" ").locationId(" ").offenderIdentifier(" ").build())
    assertThat(allCaseNotes.size).isGreaterThan(0)
    val caseNotes = repository.findAll(OffenderCaseNoteFilter.builder()
        .type(PARENT_TYPE).subType(SUB_TYPE).authorUsername("FILTER").locationId("BOB").offenderIdentifier(OFFENDER_IDENTIFIER).build())
    assertThat(caseNotes).hasSize(1)
  }

  @Test
  fun testAmendmentUpdatesCaseNoteModification() {
    val twoDaysAgo = LocalDateTime.now().minusDays(2)
    val noteText = "updates old note"
    val oldNote = repository.save(transientEntityBuilder(OFFENDER_IDENTIFIER).noteText(noteText).build())
    val noteTextWithAmendment = "updates old note with old amendment"
    val oldNoteWithOldAmendment = repository.save(transientEntityBuilder(OFFENDER_IDENTIFIER).noteText(noteTextWithAmendment).build())
    oldNoteWithOldAmendment.addAmendment("Some amendment", "someuser", "Some User", "user id")
    repository.save(oldNoteWithOldAmendment)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    // set the notes to two days ago
    val update = jdbcTemplate.update("update offender_case_note set modify_date_time = ? where offender_case_note_id in (?, ?)", twoDaysAgo,
        oldNote.id.toString(), oldNoteWithOldAmendment.id.toString())
    assertThat(update).isEqualTo(2)

    // now add an amendment
    val retrievedOldNote = repository.findById(oldNote.id).orElseThrow()
    retrievedOldNote.addAmendment("An amendment", "anotheruser", "Another User", "user id")
    repository.save(retrievedOldNote)
    val yesterday = LocalDateTime.now().minusDays(1)
    val rows = repository.findBySensitiveCaseNoteType_ParentType_TypeInAndModifyDateTimeAfterOrderByModifyDateTime(setOf("POM"), yesterday, Pageable.unpaged())
    assertThat(rows).extracting<String, RuntimeException> { obj: OffenderCaseNote -> obj.noteText }.contains(noteText)
    assertThat(rows).extracting<String, RuntimeException> { obj: OffenderCaseNote -> obj.noteText }.doesNotContain(noteTextWithAmendment)
  }

  @Test
  fun findByModifiedDate() {
    val twoDaysAgo = LocalDateTime.now().minusDays(2)
    val oldNoteText = "old note"
    val oldNote = repository.save(transientEntityBuilder(OFFENDER_IDENTIFIER).noteText(oldNoteText).build())
    val newNoteText = "new note"
    repository.save(transientEntityBuilder(OFFENDER_IDENTIFIER).noteText(newNoteText).build())
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    // set the old notes two days ago so won't be returned
    val update = jdbcTemplate.update("update offender_case_note set modify_date_time = ? where offender_case_note_id in (?)", twoDaysAgo, oldNote.id.toString())
    assertThat(update).isEqualTo(1)
    val yesterday = LocalDateTime.now().minusDays(1)
    val rows = repository.findBySensitiveCaseNoteType_ParentType_TypeInAndModifyDateTimeAfterOrderByModifyDateTime(setOf("POM", "BOB"), yesterday, Pageable.unpaged())
    assertThat(rows).extracting<String, RuntimeException> { obj: OffenderCaseNote -> obj.noteText }.contains(newNoteText)
    assertThat(rows).extracting<String, RuntimeException> { obj: OffenderCaseNote -> obj.noteText }.doesNotContain(oldNoteText)
  }

  @Test
  fun testGenerationOfEventId() {
    val note = repository.save(transientEntity(OFFENDER_IDENTIFIER))
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    assertThat(repository.findById(note.id).orElseThrow().eventId).isLessThan(0)
  }

  @Test
  fun testDeleteCaseNotes() {
    val persistedEntity = repository.save(transientEntityBuilder("X1111XX").noteText("note to delete").build())
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val deletedCaseNotes = repository.deleteOffenderCaseNoteByOffenderIdentifier("X1111XX")
    assertThat(deletedCaseNotes).isEqualTo(1)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(repository.findById(persistedEntity.id).isEmpty)
    val sql = String.format("SELECT COUNT(*) FROM offender_case_note Where offender_case_note_id = '%s'", persistedEntity.id.toString())
    val caseNoteCountAfter = jdbcTemplate.queryForObject(sql, Int::class.java)
    assertThat(caseNoteCountAfter).isEqualTo(0)
  }

  @Test
  fun testDeleteOfSoftDeletedCaseNotes() {
    val persistedEntity = repository.save(transientEntityBuilder("X2111XX").noteText("note to delete").softDeleted(true).build())
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val deletedCaseNotes = repository.deleteOffenderCaseNoteByOffenderIdentifier("X2111XX")
    assertThat(deletedCaseNotes).isEqualTo(1)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    assertThat(repository.findById(persistedEntity.id).isEmpty)
    TestTransaction.end()
    val sql = String.format("SELECT COUNT(*) FROM offender_case_note Where offender_case_note_id = '%s'", persistedEntity.id.toString())
    val caseNoteCountAfter = jdbcTemplate.queryForObject(sql, Int::class.java)
    assertThat(caseNoteCountAfter).isEqualTo(0)
  }

  @Test
  fun testDeleteOfSoftDeletedCaseNotesAmendments() {
    val persistedEntity = repository.save(transientEntityBuilder("X3111XX")
        .noteText("note to delete")
        .softDeleted(true)
        .build())
    persistedEntity.addAmendment("Another Note 0", "someuser", "Some User", "user id")
    repository.save(persistedEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    val caseNoteCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM offender_case_note_amendment", Int::class.java)
    assertThat(caseNoteCountBefore).isEqualTo(3)
    TestTransaction.start()
    repository.deleteOffenderCaseNoteAmendmentsByOffenderIdentifier("X3111XX")
    val deletedCaseNotes = repository.deleteOffenderCaseNoteByOffenderIdentifier("X3111XX")
    assertThat(deletedCaseNotes).isEqualTo(1)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    assertThat(repository.findById(persistedEntity.id).isEmpty)
    TestTransaction.end()
    val caseNoteCountAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM offender_case_note_amendment", Int::class.java)
    assertThat(caseNoteCountAfter).isEqualTo(2)
  }

  @Test
  @WithAnonymousUser
  fun testPersistCaseNoteAndAmendmentAndThenDelete() {
    val caseNote = transientEntity(OFFENDER_IDENTIFIER)
    caseNote.addAmendment("Another Note 0", "someuser", "Some User", "user id")
    val persistedEntity = repository.save(caseNote)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity = repository.findById(persistedEntity.id).orElseThrow()
    retrievedEntity.addAmendment("Another Note 1", "someuser", "Some User", "user id")
    retrievedEntity.addAmendment("Another Note 2", "someuser", "Some User", "user id")
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    repository.deleteOffenderCaseNoteAmendmentsByOffenderIdentifier(caseNote.offenderIdentifier)
    val deletedEntities = repository.deleteOffenderCaseNoteByOffenderIdentifier(caseNote.offenderIdentifier)
    assertThat(deletedEntities).isEqualTo(1)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val deletedEntity = repository.findById(persistedEntity.id)
    assertThat(deletedEntity).isEmpty
  }

  @Test
  @WithAnonymousUser
  fun testModifyOffenderIdentifier() {
    val caseNote = transientEntity("A1234ZZ")
    caseNote.addAmendment("Another Note 0", "someuser", "Some User", "user id")
    val persistedEntity = repository.save(caseNote)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedCaseNote = repository.findById(persistedEntity.id).orElseThrow()
    assertThat(retrievedCaseNote.offenderIdentifier).isEqualTo("A1234ZZ")
    TestTransaction.end()
    TestTransaction.start()
    val rows = repository.updateOffenderIdentifier("A1234ZZ", OFFENDER_IDENTIFIER)
    assertThat(rows).isEqualTo(1)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val modifiedIdentity = repository.findById(persistedEntity.id).orElseThrow()
    assertThat(modifiedIdentity.offenderIdentifier).isEqualTo(OFFENDER_IDENTIFIER)
  }

  @Test
  @WithAnonymousUser
  fun testModifyOffenderIdentifierWhenACaseNoteIsSoftDeleted() {
    val caseNote = transientEntity("A2234ZZ")
    caseNote.addAmendment("Another Note 0", "someuser", "Some User", "user id")
    val persistedEntity = repository.save(caseNote)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedCaseNote = repository.findById(persistedEntity.id).orElseThrow()
    assertThat(retrievedCaseNote.offenderIdentifier).isEqualTo("A2234ZZ")
    repository.delete(retrievedCaseNote)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedCaseNote2 = repository.findById(persistedEntity.id)
    assertThat(retrievedCaseNote2).isEmpty
    val rows = repository.updateOffenderIdentifier("A2234ZZ", OFFENDER_IDENTIFIER)
    assertThat(rows).isEqualTo(1)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    val sql = String.format("SELECT offender_identifier FROM offender_case_note Where offender_case_note_id = '%s'", persistedEntity.id.toString())
    val caseNoteOffenderIdentifierIgnoreSoftDelete = jdbcTemplate.queryForObject(sql, String::class.java)
    assertThat(caseNoteOffenderIdentifierIgnoreSoftDelete).isEqualTo(OFFENDER_IDENTIFIER)
  }

  @Test
  fun testOffenderCaseNoteSoftDeleted() {
    val caseNote = transientEntity("A2345AB")
    val persistedEntity = repository.save(caseNote)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedCaseNote = repository.findById(persistedEntity.id).orElseThrow()
    repository.delete(retrievedCaseNote)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedSoftDeleteCaseNote = repository.findById(persistedEntity.id)
    assertThat(retrievedSoftDeleteCaseNote).isEmpty
  }

  @Test
  @WithAnonymousUser
  fun testRetrieveASoftDeletedFalseCaseNote() {
    val persistedEntity = repository.save(transientEntityBuilder("X4111XX").noteText("note to retrieve").softDeleted(false).build())
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val caseNoteId = persistedEntity.id
    val caseNote = repository.findById(caseNoteId).orElseThrow()
    assertThat(caseNote.offenderIdentifier).isEqualTo("X4111XX")
    TestTransaction.end()
    val sql = String.format("SELECT offender_identifier FROM offender_case_note Where offender_case_note_id = '%s'", persistedEntity.id.toString())
    val caseNoteOffenderIdentifierIgnoreSoftDelete = jdbcTemplate.queryForObject(sql, String::class.java)
    assertThat(caseNoteOffenderIdentifierIgnoreSoftDelete).isEqualTo("X4111XX")
  }

  @Test
  @WithAnonymousUser
  fun testRetrieveASoftDeletedTrueCaseNote() {
    val persistedEntity = repository.save(transientEntityBuilder("X5111XX").noteText("note to retrieve").softDeleted(true).build())
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val caseNoteId = persistedEntity.id
    val caseNote = repository.findById(caseNoteId)
    assertThat(caseNote).isEmpty
    TestTransaction.end()
    val sql = String.format("SELECT offender_identifier FROM offender_case_note Where offender_case_note_id = '%s'", persistedEntity.id.toString())
    val caseNoteOffenderIdentifierIgnoreSoftDelete = jdbcTemplate.queryForObject(sql, String::class.java)
    assertThat(caseNoteOffenderIdentifierIgnoreSoftDelete).isEqualTo("X5111XX")
  }

  @Test
  @WithAnonymousUser
  fun testThatSoftDeleteDoesntCascadeFromCaseNoteToAmendments() {
    val persistedEntity = repository.save(transientEntityBuilder("X9111XX")
        .noteText("note to delete")
        .build())
    persistedEntity.addAmendment("Another Note 0", "someuser", "Some User", "user id")
    persistedEntity.addAmendment("Another Note 1", "someuser", "Some User", "user id")
    repository.save(persistedEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity = repository.findById(persistedEntity.id).orElseThrow()
    TestTransaction.end()
    TestTransaction.start()
    repository.deleteById(retrievedEntity.id)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    val sql = String.format("SELECT soft_deleted FROM offender_case_note_amendment Where offender_case_note_amendment_id = '%s'", persistedEntity.getAmendment(1).get().id)
    assertThat(jdbcTemplate.queryForObject(sql, Boolean::class.java)).isFalse
  }

  private fun transientEntity(offenderIdentifier: String): OffenderCaseNote {
    return transientEntityBuilder(offenderIdentifier).build()
  }

  private fun transientEntityBuilder(offenderIdentifier: String): OffenderCaseNote.OffenderCaseNoteBuilder {
    return OffenderCaseNote.builder()
        .occurrenceDateTime(LocalDateTime.now())
        .locationId("MDI")
        .authorUsername("USER2")
        .authorUserId("some id")
        .authorName("Mickey Mouse")
        .offenderIdentifier(offenderIdentifier)
        .sensitiveCaseNoteType(genType)
        .noteText("HELLO")
  }

  companion object {
    private const val PARENT_TYPE = "POM"
    private const val SUB_TYPE = "GEN"
    const val OFFENDER_IDENTIFIER = "A1234BD"
  }
}
