package uk.gov.justice.hmpps.casenotes.services

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.springframework.security.access.AccessDeniedException
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.UserIdUser
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNote
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType
import uk.gov.justice.hmpps.casenotes.model.SensitiveCaseNoteType
import uk.gov.justice.hmpps.casenotes.repository.CaseNoteTypeRepository
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteAmendmentRepository
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.repository.ParentCaseNoteTypeRepository
import java.time.LocalDateTime
import java.util.*
import javax.validation.ValidationException

class CaseNoteServiceTest {

  private val repository: OffenderCaseNoteRepository = mock()
  private val amendmentRepository: OffenderCaseNoteAmendmentRepository = mock()
  private val caseNoteTypeRepository: CaseNoteTypeRepository = mock()
  private val parentCaseNoteTypeRepository: ParentCaseNoteTypeRepository = mock()
  private val securityUserContext: SecurityUserContext = mock()
  private val externalApiService: ExternalApiService = mock()
  private val caseNoteTypeMerger: CaseNoteTypeMerger = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val caseNoteService = CaseNoteService(repository, amendmentRepository, caseNoteTypeRepository, parentCaseNoteTypeRepository, securityUserContext, externalApiService, caseNoteTypeMerger, telemetryClient)

  @Test
  fun createCaseNote_callElite2() {
    whenever(caseNoteTypeRepository.findSensitiveCaseNoteTypeByParentType_TypeAndType(any(), any())).thenReturn(null)
    val nomisCaseNote = createNomisCaseNote()
    whenever(externalApiService.createCaseNote(any(), any())).thenReturn(nomisCaseNote)
    val caseNote = caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("SUB").build())
    assertThat(caseNote).isEqualToIgnoringGivenFields(nomisCaseNote, "authorUsername", "locationId", "text", "caseNoteId", "authorUserId", "eventId")
    assertThat(caseNote.text).isEqualTo("original")
    assertThat(caseNote.authorUserId).isEqualTo("23456")
    assertThat(caseNote.locationId).isEqualTo("agency")
    assertThat(caseNote.caseNoteId).isEqualTo("12345")
    assertThat(caseNote.eventId).isEqualTo(12345)
    verify(caseNoteTypeRepository).findSensitiveCaseNoteTypeByParentType_TypeAndType("type", "SUB")
  }

  @Test
  fun createCaseNote_noAddRole() {
    whenever(caseNoteTypeRepository.findSensitiveCaseNoteTypeByParentType_TypeAndType(any(), any())).thenReturn(SensitiveCaseNoteType.builder().build())
    whenever(securityUserContext.isOverrideRole(any(), any())).thenReturn(false)
    assertThatThrownBy { caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("SUB").build()) }.isInstanceOf(AccessDeniedException::class.java)
    verify(securityUserContext).isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES")
  }

  @Test
  fun createCaseNote() {
    val noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    whenever(caseNoteTypeRepository.findSensitiveCaseNoteTypeByParentType_TypeAndType(any(), any())).thenReturn(noteType)
    whenever(securityUserContext.isOverrideRole(any(), any())).thenReturn(true)
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("someuser", "userId"))
    val offenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.save(any())).thenReturn(offenderCaseNote)
    val createdNote = caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("sub").build())
    assertThat(createdNote).isEqualToIgnoringGivenFields(offenderCaseNote,
        "caseNoteId", "type", "typeDescription", "subType", "subTypeDescription", "source", "creationDateTime", "text")
    assertThat(createdNote.text).isEqualTo("HELLO")
  }

  @Test
  fun getCaseNote_noAddRole() {
    assertThatThrownBy { caseNoteService.getCaseNote("12345", UUID.randomUUID().toString()) }.isInstanceOf(AccessDeniedException::class.java)
    verify(securityUserContext).isOverrideRole("POM", "VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")
  }

  @Test
  fun getCaseNote_notFound() {
    whenever(securityUserContext.isOverrideRole(any(), any(), any())).thenReturn(true)
    assertThatThrownBy { caseNoteService.getCaseNote("12345", UUID.randomUUID().toString()) }.isInstanceOf(EntityNotFoundException::class.java)
  }

  @Test
  fun getCaseNote() {
    val noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(offenderCaseNote))
    whenever(securityUserContext.isOverrideRole(any(), any(), any())).thenReturn(true)
    val caseNote = caseNoteService.getCaseNote("12345", UUID.randomUUID().toString())
    assertThat(caseNote).isEqualToIgnoringGivenFields(offenderCaseNote,
        "caseNoteId", "type", "typeDescription", "subType", "subTypeDescription", "source", "creationDateTime", "authorUsername", "authorName", "text")
    assertThat(caseNote.text).isEqualTo("HELLO")
  }

  @Test
  fun caseNote_callElite2() {
    val nomisCaseNote = createNomisCaseNote()
    whenever(externalApiService.getOffenderCaseNote(any(), ArgumentMatchers.anyLong())).thenReturn(nomisCaseNote)
    val caseNote = caseNoteService.getCaseNote("12345", "21455")
    assertThat(caseNote).isEqualToIgnoringGivenFields(nomisCaseNote, "authorUsername", "locationId", "text", "caseNoteId", "authorUserId", "eventId")
    assertThat(caseNote.text).isEqualTo("original")
    assertThat(caseNote.authorUserId).isEqualTo("23456")
    assertThat(caseNote.locationId).isEqualTo("agency")
    assertThat(caseNote.caseNoteId).isEqualTo("12345")
    assertThat(caseNote.eventId).isEqualTo(12345)
  }

  @Test
  fun amendCaseNote_callElite2() {
    val nomisCaseNote = createNomisCaseNote()
    whenever(externalApiService.amendOffenderCaseNote(any(), ArgumentMatchers.anyLong(), ArgumentMatchers.any())).thenReturn(nomisCaseNote)
    val caseNote = caseNoteService.amendCaseNote("12345", "21455", UpdateCaseNote("text"))
    assertThat(caseNote).isEqualToIgnoringGivenFields(nomisCaseNote, "authorUsername", "locationId", "text", "caseNoteId", "authorUserId", "eventId")
    assertThat(caseNote.text).isEqualTo("original")
    assertThat(caseNote.authorUserId).isEqualTo("23456")
    assertThat(caseNote.locationId).isEqualTo("agency")
    assertThat(caseNote.caseNoteId).isEqualTo("12345")
    assertThat(caseNote.eventId).isEqualTo(12345)
  }

  @Test
  fun amendCaseNote_noAddRole() {
    assertThatThrownBy { caseNoteService.amendCaseNote("12345", UUID.randomUUID().toString(), UpdateCaseNote("text")) }.isInstanceOf(AccessDeniedException::class.java)
    verify(securityUserContext).isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES")
  }

  @Test
  fun amendCaseNote_notFound() {
    whenever(securityUserContext.isOverrideRole(any(), any())).thenReturn(true)
    val caseNoteIdentifier = UUID.randomUUID().toString()
    assertThatThrownBy { caseNoteService.amendCaseNote("12345", caseNoteIdentifier, UpdateCaseNote("text")) }
        .isInstanceOf(EntityNotFoundException::class.java).hasMessage(String.format("Resource with id [%s] not found.", caseNoteIdentifier))
  }

  @Test
  fun amendCaseNote_wrongOffender() {
    val noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))
    whenever(securityUserContext.isOverrideRole(any(), any())).thenReturn(true)
    assertThatThrownBy { caseNoteService.amendCaseNote("12345", UUID.randomUUID().toString(), UpdateCaseNote("text")) }
        .isInstanceOf(EntityNotFoundException::class.java).hasMessage("Resource with id [12345] not found.")
  }

  @Test
  fun deleteOffenderTest() {
    whenever(repository.deleteOffenderCaseNoteByOffenderIdentifier(eq("A1234AC"))).thenReturn(3)
    val offendersDeleted = caseNoteService.deleteCaseNotesForOffender("A1234AC")
    assertThat(offendersDeleted).isEqualTo(3)
  }

  @Test
  fun deleteOffenderTest_telemetry() {
    whenever(repository.deleteOffenderCaseNoteByOffenderIdentifier(eq("A1234AC"))).thenReturn(3)
    caseNoteService.deleteCaseNotesForOffender("A1234AC")
    verify(telemetryClient).trackEvent("OffenderDelete", mapOf("offenderNo" to "A1234AC", "count" to "3"), null)
  }

  @Test
  fun amendCaseNote() {
    val noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(offenderCaseNote))
    whenever(securityUserContext.isOverrideRole(any(), any())).thenReturn(true)
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("user", "userId"))
    whenever(externalApiService.getUserFullName(any())).thenReturn("author")
    val caseNote = caseNoteService.amendCaseNote("A1234AC", UUID.randomUUID().toString(), UpdateCaseNote("text"))
    assertThat(caseNote.amendments).hasSize(1)
    val expected = CaseNoteAmendment.builder()
        .additionalNoteText("text")
        .authorName("author")
        .authorUserId("some id")
        .authorUserName("user")
        .sequence(1).build()
    assertThat(caseNote.amendments[0]).isEqualToComparingOnlyGivenFields(expected, "additionalNoteText", "authorName", "authorUserName", "sequence")
  }

  @Test
  fun softDeleteCaseNote() {
    val noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    val offenderCaseNoteId = offenderCaseNote.id
    whenever(repository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(offenderCaseNote))
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("user", "userId"))
    caseNoteService.softDeleteCaseNote("A1234AC", offenderCaseNoteId.toString())
    verify(repository).deleteById(offenderCaseNoteId)
  }

  @Test
  fun softDeleteCaseNote_telemetry() {
    val noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    val offenderCaseNoteId = offenderCaseNote.id
    whenever(repository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(offenderCaseNote))
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("user", "userId"))
    caseNoteService.softDeleteCaseNote("A1234AC", offenderCaseNoteId.toString())
    verify(telemetryClient).trackEvent("SecureCaseNoteSoftDelete", mapOf("userName" to "user", "offenderId" to "A1234AC", "case note id" to offenderCaseNoteId.toString()), null)
  }

  @Test
  fun softDeleteCaseNoteEntityNotFoundExceptionThrownWhenCaseNoteNotFound() {
    assertThatThrownBy { caseNoteService.softDeleteCaseNote("A1234AC", UUID.randomUUID().toString()) }.isInstanceOf(EntityNotFoundException::class.java)
  }

  @Test
  fun softDeleteCaseNoteEntityNotFoundExceptionThrownWhenCaseNoteDoesntBelongToOffender() {
    val noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    val offenderCaseNoteId = offenderCaseNote.id
    whenever(repository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(offenderCaseNote))
    assertThatThrownBy { caseNoteService.softDeleteCaseNote("Z9999ZZ", offenderCaseNoteId.toString()) }.isInstanceOf(ValidationException::class.java)
  }

  @Test
  fun softDeleteCaseNoteAmendment() {
    val noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNoteAmendment = createOffenderCaseNoteAmendment(noteType)
    whenever(amendmentRepository.findById(1L)).thenReturn(Optional.of(offenderCaseNoteAmendment))
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("user", "userId"))
    caseNoteService.softDeleteCaseNoteAmendment("A1234AC", 1L)
    verify(amendmentRepository).deleteById(1L)
  }

  @Test
  fun softDeleteCaseNoteAmendment_telemetry() {
    val noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNoteAmendment = createOffenderCaseNoteAmendment(noteType)
    whenever(amendmentRepository.findById(1L)).thenReturn(Optional.of(offenderCaseNoteAmendment))
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("user", "userId"))
    caseNoteService.softDeleteCaseNoteAmendment("A1234AC", 1L)
    verify(telemetryClient).trackEvent("SecureCaseNoteAmendmentSoftDelete", mapOf("userName" to "user", "offenderId" to "A1234AC", "case note amendment id" to "1"), null)
  }

  @Test
  fun softDeleteCaseNoteAmendmentEntityNotFoundExceptionThrownWhenCaseNoteNotFound() {
    assertThatThrownBy { caseNoteService.softDeleteCaseNoteAmendment("A1234AC", 1L) }.isInstanceOf(EntityNotFoundException::class.java)
  }

  @Test
  fun softDeleteCaseNoteAmendmentEntityNotFoundExceptionThrownWhenCaseNoteDoesntBelongToOffender() {
    val noteType = SensitiveCaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNoteAmendment = createOffenderCaseNoteAmendment(noteType)
    whenever(amendmentRepository.findById(any())).thenReturn(Optional.of(offenderCaseNoteAmendment))
    assertThatThrownBy { caseNoteService.softDeleteCaseNoteAmendment("Z9999ZZ", 1L) }.isInstanceOf(ValidationException::class.java)
  }

  private fun createNomisCaseNote() = NomisCaseNote.builder()
      .agencyId("agency")
      .authorName("somebody")
      .caseNoteId(12345)
      .creationDateTime(LocalDateTime.parse("2019-03-23T11:22"))
      .occurrenceDateTime(LocalDateTime.parse("2019-04-16T10:42"))
      .originalNoteText("original")
      .source("WHERE")
      .staffId(23456L)
      .subType("SUB")
      .subTypeDescription("Sub desc")
      .text("new text")
      .type("type")
      .typeDescription("Type desc")
      .offenderIdentifier("12345")
      .build()


  private fun createOffenderCaseNote(caseNoteType: SensitiveCaseNoteType) = OffenderCaseNote.builder()
      .id(UUID.randomUUID())
      .occurrenceDateTime(LocalDateTime.now())
      .locationId("MDI")
      .authorUsername("USER2")
      .authorUserId("some user")
      .authorName("Mickey Mouse")
      .offenderIdentifier("A1234AC")
      .sensitiveCaseNoteType(caseNoteType)
      .noteText("HELLO")
      .build()


  private fun createOffenderCaseNoteAmendment(caseNoteType: SensitiveCaseNoteType) =
      OffenderCaseNoteAmendment
          .builder()
          .caseNote(createOffenderCaseNote(caseNoteType))
          .id(1L)
          .noteText("A")
          .authorName("some user")
          .build()

}
