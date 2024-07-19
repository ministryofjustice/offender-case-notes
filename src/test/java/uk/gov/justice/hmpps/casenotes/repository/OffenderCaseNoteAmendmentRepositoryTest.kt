package uk.gov.justice.hmpps.casenotes.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.hmpps.casenotes.config.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.casenotes.health.IntegrationTest
import uk.gov.justice.hmpps.casenotes.model.CaseNoteType
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import java.time.LocalDateTime

@Transactional
class OffenderCaseNoteAmendmentRepositoryTest : IntegrationTest() {
  @Autowired
  private lateinit var repository: OffenderCaseNoteRepository

  @Autowired
  private lateinit var amendmentRepository: OffenderCaseNoteAmendmentRepository

  @Autowired
  private lateinit var caseNoteTypeRepository: CaseNoteTypeRepository
  private lateinit var genType: CaseNoteType

  @BeforeEach
  fun setUp() {
    val jwt = Jwt.withTokenValue("some").subject("anonymous").header("head", "something").build()
    SecurityContextHolder.getContext().authentication = AuthAwareAuthenticationToken(jwt, "userId", emptyList())
    genType = caseNoteTypeRepository.findCaseNoteTypeByParentType_TypeAndType(PARENT_TYPE, SUB_TYPE)!!
  }

  @Test
  @WithAnonymousUser
  fun testOffenderCaseNoteAmendmentSoftDeleted() {
    val caseNote = transientEntity("A2345BB")
    caseNote.addAmendment("Another Note 0", "someuser", "Some User", "user id")
    val persistedEntity = repository.save(caseNote)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    val amendmentId = repository.findById(persistedEntity.id).orElseThrow().amendments.first().id

    TestTransaction.end()
    TestTransaction.start()
    amendmentRepository.deleteById(amendmentId)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    val retrievedCaseNote = repository.findById(persistedEntity.id).orElseThrow()
    assertThat(retrievedCaseNote.amendments).isEmpty()
  }

  @Test
  @WithAnonymousUser
  fun `test adding a new offender case note amendment after deleting an amendment `() {
    val caseNote = transientEntity("A2345BB")
    caseNote.addAmendment("Another Note 0", "someuser", "Some User", "user id")
    val persistedEntity = repository.save(caseNote)
    TestTransaction.flagForCommit()
    TestTransaction.end()

    TestTransaction.start()
    val amendmentId = repository.findById(persistedEntity.id).orElseThrow().amendments.first().id
    TestTransaction.end()

    TestTransaction.start()
    amendmentRepository.deleteById(amendmentId)
    TestTransaction.flagForCommit()
    TestTransaction.end()

    TestTransaction.start()
    val retrievedCaseNote = repository.findById(persistedEntity.id).orElseThrow()
    assertThat(retrievedCaseNote.amendments).isEmpty()
    TestTransaction.end()

    TestTransaction.start()
    retrievedCaseNote.addAmendment("Another Note 1", "someuser", "Some User", "user id")
    repository.save(retrievedCaseNote)
    TestTransaction.flagForCommit()
    TestTransaction.end()

    TestTransaction.start()
    val additionalAmendment = repository.findById(persistedEntity.id).orElseThrow().amendments.first()
    assertThat(additionalAmendment.noteText).isEqualTo("Another Note 1")
    TestTransaction.end()
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
      .caseNoteType(genType)
      .noteText("HELLO")
  }

  companion object {
    private const val PARENT_TYPE = "POM"
    private const val SUB_TYPE = "GEN"
  }
}
