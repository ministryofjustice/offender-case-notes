package uk.gov.justice.hmpps.casenotes.services

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.access.AccessDeniedException
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.UserIdUser
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNote
import uk.gov.justice.hmpps.casenotes.model.CaseNoteType
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType
import uk.gov.justice.hmpps.casenotes.repository.CaseNoteTypeRepository
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteAmendmentRepository
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.repository.ParentCaseNoteTypeRepository
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.assert
import kotlin.to

@ExtendWith(MockitoExtension::class)
class CaseNoteServiceTest {
  private val repository: OffenderCaseNoteRepository = mock()
  private val amendmentRepository: OffenderCaseNoteAmendmentRepository = mock()
  private val caseNoteTypeRepository: CaseNoteTypeRepository = mock()
  private val parentCaseNoteTypeRepository: ParentCaseNoteTypeRepository = mock()
  private val securityUserContext: SecurityUserContext = mock()
  private val externalApiService: ExternalApiService = mock()
  private val caseNoteTypeMerger: CaseNoteTypeMerger = mock()
  private val telemetryClient: TelemetryClient = mock()
  private var caseNoteService: CaseNoteService = mock()

  @BeforeEach
  fun setUp() {
    caseNoteService = CaseNoteService(
      repository,
      amendmentRepository,
      caseNoteTypeRepository,
      parentCaseNoteTypeRepository,
      securityUserContext,
      externalApiService,
      caseNoteTypeMerger,
      telemetryClient,
    )
  }

  @Test
  fun createCaseNote_callElite2() {
    whenever(caseNoteTypeRepository.findCaseNoteTypeByParentType_TypeAndType(any<String>(), any<String>())).thenReturn(null)
    val nomisCaseNote: NomisCaseNote = createNomisCaseNote()
    whenever(externalApiService.createCaseNote(any<String>(), any())).thenReturn(nomisCaseNote)

    val caseNote = caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("SUB").build())

    assertThat(caseNote).usingRecursiveComparison()
      .ignoringFields(
        "authorUsername",
        "locationId",
        "text",
        "caseNoteId",
        "authorUserId",
        "eventId",
        "sensitive",
      )
      .isEqualTo(nomisCaseNote)
    assertThat(caseNote.text).isEqualTo("original")
    assertThat(caseNote.authorUserId).isEqualTo("23456")
    assertThat(caseNote.locationId).isEqualTo("agency")
    assertThat(caseNote.caseNoteId).isEqualTo("12345")
    assertThat(caseNote.eventId).isEqualTo(12345)
    Mockito.verify(caseNoteTypeRepository).findCaseNoteTypeByParentType_TypeAndType("type", "SUB")
  }

  @Test
  fun createCaseNote_noAddRole() {
    whenever(caseNoteTypeRepository.findCaseNoteTypeByParentType_TypeAndType(any<String>(), any<String>())).thenReturn(CaseNoteType.builder().build())
    whenever(securityUserContext.isOverrideRole(any<String>(), any<String>())).thenReturn(false)

    assertThatThrownBy { caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("SUB").build()) }.isInstanceOf(AccessDeniedException::class.java)

    Mockito.verify(securityUserContext).isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES")
  }

  @Test
  fun createCaseNote() {
    val noteType: CaseNoteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    whenever(caseNoteTypeRepository.findCaseNoteTypeByParentType_TypeAndType(any<String>(), any<String>())).thenReturn(noteType)
    whenever(securityUserContext.isOverrideRole(any<String>(), any<String>())).thenReturn(true)
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("someuser", "userId"))
    val offenderCaseNote: OffenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.save(any())).thenReturn(offenderCaseNote)

    val createdNote: CaseNote = caseNoteService.createCaseNote("12345", NewCaseNote.builder().type("type").subType("sub").build())

    assertThat(createdNote).usingRecursiveComparison()
      .ignoringFields(
        "authorUsername",
        "caseNoteId",
        "type",
        "typeDescription",
        "subType",
        "subTypeDescription",
        "source",
        "creationDateTime",
        "text",
        "amendments",
        "sensitive",
      )
      .isEqualTo(offenderCaseNote)
    assertThat(createdNote.text).isEqualTo("HELLO")
  }

  @Test
  fun caseNote_noAddRole() {
    val noteType: CaseNoteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote: OffenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))

    assertThatThrownBy {
      caseNoteService.getCaseNote("12345", UUID.randomUUID().toString())
    }.isInstanceOf(AccessDeniedException::class.java)

    verify(securityUserContext).isOverrideRole("POM", "VIEW_SENSITIVE_CASE_NOTES", "ADD_SENSITIVE_CASE_NOTES")
  }

  @Test
  fun caseNote_notFound() {
    whenever(repository.findById(any())).thenReturn(Optional.empty())

    assertThatThrownBy { caseNoteService.getCaseNote("12345", UUID.randomUUID().toString()) }
      .isInstanceOf(EntityNotFoundException::class.java)
  }

  @Test
  fun getCaseNote() {
    val noteType: CaseNoteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote: OffenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))
    whenever(securityUserContext.isOverrideRole(any<String>(), any<String>(), any<String>())).thenReturn(true)

    val caseNote: CaseNote = caseNoteService.getCaseNote("12345", UUID.randomUUID().toString())
    assertThat(caseNote).usingRecursiveComparison()
      .ignoringFields(
        "caseNoteId",
        "type",
        "typeDescription",
        "subType",
        "subTypeDescription",
        "source",
        "creationDateTime",
        "authorUsername",
        "authorName",
        "text",
        "amendments",
        "sensitive",
      )
      .isEqualTo(offenderCaseNote)
    assertThat(caseNote.text).isEqualTo("HELLO")
  }

  @Test
  fun caseNote_callElite2() {
    val nomisCaseNote: NomisCaseNote = createNomisCaseNote()
    whenever(externalApiService.getOffenderCaseNote(any<String>(), any<Long>())).thenReturn(nomisCaseNote)

    val caseNote: CaseNote = caseNoteService.getCaseNote("12345", "21455")

    assertThat(caseNote).usingRecursiveComparison()
      .ignoringFields(
        "authorUsername",
        "locationId",
        "text",
        "caseNoteId",
        "authorUserId",
        "eventId",
        "sensitive",
      )
      .isEqualTo(nomisCaseNote)
    assertThat(caseNote.text).isEqualTo("original")
    assertThat(caseNote.authorUserId).isEqualTo("23456")
    assertThat(caseNote.locationId).isEqualTo("agency")
    assertThat(caseNote.caseNoteId).isEqualTo("12345")
    assertThat(caseNote.eventId).isEqualTo(12345)
  }

  @Test
  fun caseNoteWithAmendment_callElite2() {
    val nomisCaseNote: NomisCaseNote = createNomisCaseNote()
    nomisCaseNote.amendments = listOf(
      NomisCaseNoteAmendment.builder()
        .additionalNoteText("additional details")
        .authorUsername("AAA11B")
        .creationDateTime(LocalDateTime.parse("2019-03-24T11:22"))
        .build(),
    )
    whenever(externalApiService.getOffenderCaseNote(any<String>(), any<Long>())).thenReturn(nomisCaseNote)

    val caseNote: CaseNote = caseNoteService.getCaseNote("12345", "21455")

    assertThat(caseNote)
      .usingRecursiveComparison()
      .ignoringFields(
        "authorUsername",
        "locationId",
        "text",
        "caseNoteId",
        "authorUserId",
        "eventId",
        "sensitive",
        "amendments",
      )
      .isEqualTo(nomisCaseNote)
    assertThat(caseNote.text).isEqualTo("original")
    assertThat(caseNote.authorUserId).isEqualTo("23456")
    assertThat(caseNote.locationId).isEqualTo("agency")
    assertThat(caseNote.caseNoteId).isEqualTo("12345")
    assertThat(caseNote.eventId).isEqualTo(12345)
    assertThat(caseNote.amendments.get(0).additionalNoteText).isEqualTo("additional details")
    assertThat(caseNote.amendments.get(0).authorUserName).isEqualTo("AAA11B")
    assertThat(caseNote.amendments.get(0).creationDateTime).isEqualTo("2019-03-24T11:22")
  }

  @Test
  fun amendCaseNote_callElite2() {
    val nomisCaseNote: NomisCaseNote = createNomisCaseNote()
    whenever(externalApiService.amendOffenderCaseNote(any<String>(), any<Long>(), any())).thenReturn(nomisCaseNote)

    val caseNote: CaseNote = caseNoteService.amendCaseNote("12345", "21455", UpdateCaseNote("text"))

    assertThat(caseNote).usingRecursiveComparison()
      .ignoringFields(
        "authorUsername",
        "locationId",
        "text",
        "caseNoteId",
        "authorUserId",
        "eventId",
        "sensitive",
      )
      .isEqualTo(nomisCaseNote)

    assertThat(caseNote.text).isEqualTo("original")
    assertThat(caseNote.authorUserId).isEqualTo("23456")
    assertThat(caseNote.locationId).isEqualTo("agency")
    assertThat(caseNote.caseNoteId).isEqualTo("12345")
    assertThat(caseNote.eventId).isEqualTo(12345)
  }

  @Test
  fun amendCaseNote_noAddRole() {
    val noteType: CaseNoteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote: OffenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))

    assertThatThrownBy { caseNoteService.amendCaseNote("12345", UUID.randomUUID().toString(), UpdateCaseNote("text")) }
      .isInstanceOf(AccessDeniedException::class.java)

    Mockito.verify(securityUserContext).isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES")
  }

  @Test
  fun amendCaseNote_notFound() {
    val caseNoteIdentifier: String = UUID.randomUUID().toString()

    assertThatThrownBy { caseNoteService.amendCaseNote("12345", caseNoteIdentifier, UpdateCaseNote("text")) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage(String.format("Resource with id [%s] not found.", caseNoteIdentifier))
  }

  @Test
  fun amendCaseNote_wrongOffender() {
    val noteType: CaseNoteType = CaseNoteType.builder().type("sometype").restrictedUse(false).parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote: OffenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))

    assertThatThrownBy { caseNoteService.amendCaseNote("12345", UUID.randomUUID().toString(), UpdateCaseNote("text")) }
      .isInstanceOf(EntityNotFoundException::class.java).hasMessage("Resource with id [12345] not found.")
  }

  @Test
  fun deleteOffenderTest() {
    whenever(repository.deleteOffenderCaseNoteByOffenderIdentifier(eq("A1234AC"))).thenReturn(3)
    val offendersDeleted: Int = caseNoteService.deleteCaseNotesForOffender("A1234AC")
    assertThat(offendersDeleted).isEqualTo(3)
  }

  @Test
  fun deleteOffenderTest_telemetry() {
    whenever(repository.deleteOffenderCaseNoteByOffenderIdentifier(eq("A1234AC")))
      .thenReturn(3)
    caseNoteService.deleteCaseNotesForOffender("A1234AC")
    Mockito.verify(telemetryClient).trackEvent("OffenderDelete", mapOf("offenderNo" to "A1234AC", "count" to "3"), null)
  }

  @Test
  fun amendCaseNote() {
    val noteType: CaseNoteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote: OffenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))
    whenever(securityUserContext.isOverrideRole(any<String>(), any<String>())).thenReturn(true)
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("user", "userId"))
    whenever(externalApiService.getUserFullName(any<String>())).thenReturn("author")

    val caseNote: CaseNote = caseNoteService.amendCaseNote("A1234AC", UUID.randomUUID().toString(), UpdateCaseNote("text"))
    assertThat(caseNote.amendments).hasSize(1)
    val expected: CaseNoteAmendment = CaseNoteAmendment.builder()
      .additionalNoteText("text")
      .authorName("author")
      .authorUserId("some id")
      .authorUserName("user")
      .build()
    assertThat(caseNote.amendments[0]).usingRecursiveComparison()
      .comparingOnlyFields(
        "additionalNoteText",
        "authorName",
        "authorUserName",
      )
      .isEqualTo(expected)
  }

  @Test
  fun softDeleteCaseNote() {
    val noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    val offenderCaseNoteId = offenderCaseNote.id
    whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("user", "userId"))

    caseNoteService.softDeleteCaseNote("A1234AC", offenderCaseNoteId.toString())

    Mockito.verify(repository).deleteById(offenderCaseNoteId)
  }

  @Test
  fun softDeleteCaseNote_telemetry() {
    val noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    val offenderCaseNoteId = offenderCaseNote.id
    whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("user", "userId"))

    caseNoteService.softDeleteCaseNote("A1234AC", offenderCaseNoteId.toString())

    Mockito.verify(telemetryClient).trackEvent("SecureCaseNoteSoftDelete", mapOf("userName" to "user", "offenderId" to "A1234AC", "case note id" to offenderCaseNoteId.toString()), null)
  }

  @Test
  fun softDeleteCaseNoteEntityNotFoundExceptionThrownWhenCaseNoteNotFound() {
    assertThatThrownBy { caseNoteService.softDeleteCaseNote("A1234AC", UUID.randomUUID().toString()) }
      .isInstanceOf(EntityNotFoundException::class.java)
  }

  @Test
  fun softDeleteCaseNoteEntityNotFoundExceptionThrownWhenCaseNoteDoesntBelongToOffender() {
    val noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    val offenderCaseNoteId = offenderCaseNote.id
    whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))

    assertThatThrownBy { caseNoteService.softDeleteCaseNote("Z9999ZZ", offenderCaseNoteId.toString()) }
      .isInstanceOf(ValidationException::class.java)
  }

  @Test
  fun softDeleteCaseNoteAmendment() {
    val noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNoteAmendment = createOffenderCaseNoteAmendment(noteType)
    whenever(amendmentRepository.findById(1L)).thenReturn(offenderCaseNoteAmendment)
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("user", "userId"))

    caseNoteService.softDeleteCaseNoteAmendment("A1234AC", 1L)

    verify(amendmentRepository).deleteById(1L)
  }

  @Test
  fun softDeleteCaseNoteAmendment_telemetry() {
    val noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNoteAmendment = createOffenderCaseNoteAmendment(noteType)
    whenever(amendmentRepository.findById(1L)).thenReturn(offenderCaseNoteAmendment)
    whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("user", "userId"))

    caseNoteService.softDeleteCaseNoteAmendment("A1234AC", 1L)

    Mockito.verify(telemetryClient).trackEvent("SecureCaseNoteAmendmentSoftDelete", mapOf("userName" to "user", "offenderId" to "A1234AC", "case note amendment id" to "1"), null)
  }

  @Test
  fun softDeleteCaseNoteAmendmentEntityNotFoundExceptionThrownWhenCaseNoteNotFound() {
    assertThatThrownBy { caseNoteService.softDeleteCaseNoteAmendment("A1234AC", 1L) }
      .isInstanceOf(EntityNotFoundException::class.java)
  }

  @Test
  fun softDeleteCaseNoteAmendmentEntityNotFoundExceptionThrownWhenCaseNoteDoesntBelongToOffender() {
    val noteType = CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
    val offenderCaseNoteAmendment = createOffenderCaseNoteAmendment(noteType)
    whenever(amendmentRepository.findById(any<Long>())).thenReturn(offenderCaseNoteAmendment)

    assertThatThrownBy { caseNoteService.softDeleteCaseNoteAmendment("Z9999ZZ", 1L) }
      .isInstanceOf(ValidationException::class.java)
  }

  @Test
  fun getCaseNotes_callPrisonerApi() {
    val nomisCaseNote = createNomisCaseNote("someType", "someSubType")
    val pageable = PageRequest.of(0, 10, Direction.DESC, "occurrenceDateTime")
    val apiResponseList = listOf(nomisCaseNote)
    val repositoryResponseList = listOf<OffenderCaseNote>()
    whenever(repository.findAll(ArgumentMatchers.any<Specification<OffenderCaseNote>>())).thenReturn(repositoryResponseList)
    whenever(externalApiService.getOffenderCaseNotes(anyString(), any(), any())).thenReturn(PageImpl(apiResponseList, pageable, 1))
    whenever(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(true)

    val filter = CaseNoteFilter("someType", "someSubType")
    val caseNotes = caseNoteService.getCaseNotes("12345", filter, pageable).content
    assertThat(caseNotes.size).isEqualTo(1)
    assert(caseNotes.stream().allMatch { x: CaseNote -> x.type == "someType" && x.subType == "someSubType" })
    assert(caseNotes.stream().anyMatch { x: CaseNote -> x.text == "original" })
  }

  @Test
  fun getCaseNotes_GetFromRepositoryAndPrisonApi() {
    val noteType = CaseNoteType.builder().type("someSubType").parentType(ParentNoteType.builder().type("someType").build()).build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    val pageable = PageRequest.of(0, 10, Direction.DESC, "occurrenceDateTime")
    whenever(repository.findAll(ArgumentMatchers.any<Specification<OffenderCaseNote>>())).thenReturn(listOf(offenderCaseNote))
    val apiResponseList = listOf(createNomisCaseNote("someType", "someSubType"))
    whenever(externalApiService.getOffenderCaseNotes(anyString(), any(), any())).thenReturn(PageImpl(apiResponseList, pageable, 1))
    whenever(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(true)
    val filter = CaseNoteFilter("someType", "someSUbType")

    val caseNotes = caseNoteService.getCaseNotes("12345", filter, pageable).content

    assertThat(caseNotes.size).isEqualTo(2)
    assert(caseNotes.stream().allMatch { x: CaseNote -> x.type == "someType" && x.subType == "someSubType" })
    assert(caseNotes.stream().anyMatch { x: CaseNote -> x.text == "HELLO" })
    assert(caseNotes.stream().anyMatch { x: CaseNote -> x.text == "original" })
  }

  @Test
  fun getCaseNotes_multipleTypes() {
    val pageable = PageRequest.of(0, 10, Direction.DESC, "occurrenceDateTime")

    val nomisCaseNote = createNomisCaseNote("someType", "someSubType")
    val addNomisCaseNote = createNomisCaseNote("someAddType", "someAddSubType")
    whenever(externalApiService.getOffenderCaseNotes(anyString(), any(), any())).thenReturn(PageImpl(listOf(nomisCaseNote, addNomisCaseNote), pageable, 2))

    val noteType = CaseNoteType.builder().type("someSubType").parentType(ParentNoteType.builder().type("someType").build()).build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.findAll(any<Specification<OffenderCaseNote>>())).thenReturn(listOf(offenderCaseNote))

    whenever(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(true)

    val filter = CaseNoteFilter(typeSubTypes = listOf("someType+someSubType", "someAddType+someAddSubType"))
    val caseNotes = caseNoteService.getCaseNotes("12345", filter, pageable).content
    assertThat(caseNotes.size).isEqualTo(3)
    assertThat(
      caseNotes.stream().filter { x: CaseNote -> x.type == "someType" && x.subType == "someSubType" }
        .count(),
    ).isEqualTo(2)
    assertThat(
      caseNotes.stream().filter { x: CaseNote -> x.type == "someAddType" && x.subType == "someAddSubType" }
        .count(),
    ).isEqualTo(1)
  }

  private fun createNomisCaseNote(): NomisCaseNote {
    return NomisCaseNote.builder()
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
  }

  private fun createNomisCaseNote(type: String, subType: String): NomisCaseNote {
    return NomisCaseNote.builder()
      .agencyId("agency")
      .authorName("somebody")
      .caseNoteId(12345)
      .creationDateTime(LocalDateTime.parse("2019-03-23T11:22"))
      .occurrenceDateTime(LocalDateTime.parse("2019-04-16T10:42"))
      .originalNoteText("original")
      .source("WHERE")
      .staffId(23456L)
      .subType(subType)
      .subTypeDescription("Sub desc")
      .text("new text")
      .type(type)
      .typeDescription("Type desc")
      .offenderIdentifier("12345")
      .build()
  }

  private fun createOffenderCaseNote(caseNoteType: CaseNoteType): OffenderCaseNote {
    return OffenderCaseNote.builder()
      .id(UUID.randomUUID())
      .occurrenceDateTime(LocalDateTime.now())
      .locationId("MDI")
      .authorUsername("USER2")
      .authorUserId("some user")
      .authorName("Mickey Mouse")
      .offenderIdentifier("A1234AC")
      .caseNoteType(caseNoteType)
      .noteText("HELLO")
      .build()
  }

  private fun createOffenderCaseNoteAmendment(caseNoteType: CaseNoteType): Optional<OffenderCaseNoteAmendment> {
    val amendment = OffenderCaseNoteAmendment
      .builder()
      .caseNote(createOffenderCaseNote(caseNoteType))
      .id(1L)
      .noteText("A")
      .authorName("some user")
      .build()

    return Optional.of(amendment)
  }
}
