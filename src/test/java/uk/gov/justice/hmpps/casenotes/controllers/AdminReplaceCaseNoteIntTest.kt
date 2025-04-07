package uk.gov.justice.hmpps.casenotes.controllers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_ADMIN
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_CASE_NOTES_WRITE
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.domain.DeletionCause
import uk.gov.justice.hmpps.casenotes.domain.Note
import uk.gov.justice.hmpps.casenotes.domain.System
import uk.gov.justice.hmpps.casenotes.domain.audit.DeletedCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.events.PersonCaseNoteEvent
import uk.gov.justice.hmpps.casenotes.health.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.ReplaceAmendmentRequest
import uk.gov.justice.hmpps.casenotes.notes.ReplaceNoteRequest
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator.personIdentifier
import uk.gov.justice.hmpps.casenotes.utils.verifyAgainst
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class AdminReplaceCaseNoteIntTest : IntegrationTest() {

  @Autowired
  lateinit var deletedCaseNoteRepository: DeletedCaseNoteRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.put().uri(BASE_URL, personIdentifier(), UUID.randomUUID()).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - does not have the right role`() {
    replaceCaseNote(
      personIdentifier(),
      UUID.randomUUID(),
      replaceNoteRequest(),
      ROLE_CASE_NOTES_WRITE,
    ).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - field validation failures`() {
    val personIdentifier = personIdentifier()
    val id = UUID.randomUUID()
    val request = replaceNoteRequest(
      type = "n".repeat(13),
      subType = "n".repeat(13),
      text = "",
      amendments = listOf(ReplaceAmendmentRequest(UUID.randomUUID(), "")),
    )
    val response = replaceCaseNote(personIdentifier, id, request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(developerMessage).isEqualTo(
        """
        |400 BAD_REQUEST Validation failures: 
        |amendment text cannot be blank
        |note text cannot be blank
        |sub type must be no more than 12 characters
        |type must be no more than 12 characters
        |
        """.trimMargin(),
      )
    }
  }

  @Test
  fun `404 not found - admin attempts to amends an amendment that does not exist`() {
    val personIdentifier = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(personIdentifier).withAmendment())
    val firstAmendment = existing.amendments().first()
    val request = existing.replaceRequest().copy(
      amendments = listOf(ReplaceAmendmentRequest(UUID.randomUUID(), firstAmendment.text)),
    )

    val response = replaceCaseNote(personIdentifier, existing.id, request).errorResponse(HttpStatus.NOT_FOUND)
    assertThat(response.developerMessage).isEqualTo("No amendment for this case note with id ${request.amendments.first().id}")
  }

  @Test
  fun `200 ok - admin replaces an existing case note`() {
    val personIdentifier = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(personIdentifier))
    val request = existing.replaceRequest().copy(
      type = "OMIC",
      subType = "GEN",
      text = "The text was updated by CENTRAL ADMIN",
      occurrenceDateTime = now(),
    )
    val response = replaceCaseNote(personIdentifier, existing.id, request).success<CaseNote>(HttpStatus.OK)
    assertThat(response.text).isEqualTo(request.text)
    assertThat(response.type).isEqualTo(request.type)
    assertThat(response.subType).isEqualTo(request.subType)
    assertThat(response.occurredAt).isEqualTo(request.occurrenceDateTime)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(existing.id, personIdentifier))
    saved.verifyAgainst(request)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(DeletionCause.UPDATE)
    assertThat(deleted.system).isEqualTo(System.DPS)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.DPS, saved)
  }

  @Test
  fun `200 ok - admin replaces an existing case note with amendments`() {
    val personIdentifier = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(personIdentifier).withAmendment().withAmendment())
    val request = existing.replaceRequest().copy(text = "The text was updated by CENTRAL ADMIN")

    val response = replaceCaseNote(personIdentifier, existing.id, request).success<CaseNote>(HttpStatus.OK)
    assertThat(response.text).isEqualTo(request.text)
    assertThat(response.amendments).hasSize(2)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(existing.id, personIdentifier))
    saved.verifyAgainst(request)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(DeletionCause.UPDATE)
    assertThat(deleted.system).isEqualTo(System.DPS)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.DPS, saved)
  }

  @Test
  fun `200 ok - admin replaces an existing case note and removes an amendment`() {
    val personIdentifier = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(personIdentifier).withAmendment().withAmendment())
    val firstAmendment = existing.amendments().first()
    val request = existing.replaceRequest().copy(
      text = "The text was updated by CENTRAL ADMIN",
      amendments = listOf(ReplaceAmendmentRequest(firstAmendment.id, firstAmendment.text)),
    )

    val response = replaceCaseNote(personIdentifier, existing.id, request).success<CaseNote>(HttpStatus.OK)
    assertThat(response.text).isEqualTo(request.text)
    assertThat(response.amendments).hasSize(1)
    assertThat(response.amendments.first().additionalNoteText).isEqualTo(request.amendments.first().text)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(existing.id, personIdentifier))
    saved.verifyAgainst(request)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(DeletionCause.UPDATE)
    assertThat(deleted.system).isEqualTo(System.DPS)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.DPS, saved)
  }

  @Test
  fun `200 ok - admin amends an amendment`() {
    val personIdentifier = personIdentifier()
    val existing = givenCaseNote(generateCaseNote(personIdentifier).withAmendment())
    val firstAmendment = existing.amendments().first()
    val request = existing.replaceRequest().copy(
      amendments = listOf(ReplaceAmendmentRequest(firstAmendment.id, "This text was updated by CENTRAL ADMIN")),
    )

    val response = replaceCaseNote(personIdentifier, existing.id, request).success<CaseNote>(HttpStatus.OK)
    assertThat(response.text).isEqualTo(request.text)
    assertThat(response.amendments).hasSize(1)
    assertThat(response.amendments.first().additionalNoteText).isEqualTo(request.amendments.first().text)

    val saved = requireNotNull(noteRepository.findByIdAndPersonIdentifier(existing.id, personIdentifier))
    saved.verifyAgainst(request)

    val deleted = deletedCaseNoteRepository.findByCaseNoteId(existing.id)
    assertThat(deleted!!.caseNote).isNotNull()
    assertThat(deleted.cause).isEqualTo(DeletionCause.UPDATE)
    assertThat(deleted.system).isEqualTo(System.DPS)
    deleted.caseNote.verifyAgainst(existing)

    hmppsEventsQueue.receivePersonCaseNoteEvent().verifyAgainst(PersonCaseNoteEvent.Type.UPDATED, Source.DPS, saved)
  }

  private fun replaceNoteRequest(
    type: String = "OMIC",
    subType: String = "GEN",
    occurrenceDateTime: LocalDateTime = now().minusDays(2),
    text: String = "The text of the case note",
    reason: String = "Reason for replacing the case note",
    amendments: List<ReplaceAmendmentRequest> = listOf(),
  ) = ReplaceNoteRequest(type, subType, text, occurrenceDateTime, reason, amendments)

  private fun replaceCaseNote(
    personIdentifier: String,
    id: UUID,
    request: ReplaceNoteRequest,
    roles: String = ROLE_CASE_NOTES_ADMIN,
  ) = webTestClient.put().uri(BASE_URL, personIdentifier, id)
    .bodyValue(request)
    .headers(addBearerAuthorisation("AdminUser", listOf(roles)))
    .exchange()

  fun Note.replaceRequest(reason: String = "Reason for replacing") = ReplaceNoteRequest(
    subType.typeCode,
    subType.code,
    text,
    occurredAt,
    reason,
    amendments().map { ReplaceAmendmentRequest(it.id, it.text) },
  )

  fun Note.verifyAgainst(request: ReplaceNoteRequest) {
    assertThat(subType.type.code).isEqualTo(request.type)
    assertThat(subType.code).isEqualTo(request.subType)
    assertThat(text).isEqualTo(request.text)
    assertThat(occurredAt.truncatedTo(SECONDS)).isEqualTo(request.occurrenceDateTime.truncatedTo(SECONDS))
    assertThat(amendments().map { it.id }).containsExactlyInAnyOrderElementsOf(request.amendments.map { it.id })
  }

  companion object {
    private const val BASE_URL = "/admin/case-notes/{personIdentifier}/{id}"

    @BeforeAll
    @JvmStatic
    fun setUp() {
      manageUsersApi.stubGetUserDetails("AdminUser")
    }
  }
}
