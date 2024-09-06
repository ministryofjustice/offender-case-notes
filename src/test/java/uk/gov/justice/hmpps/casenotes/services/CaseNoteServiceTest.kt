package uk.gov.justice.hmpps.casenotes.services

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityManager
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.jpa.domain.Specification
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.UserIdUser
import uk.gov.justice.hmpps.casenotes.config.Source
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.filters.OffenderCaseNoteFilter
import uk.gov.justice.hmpps.casenotes.model.CaseNoteType
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNote.AmendmentComparator
import uk.gov.justice.hmpps.casenotes.model.OffenderCaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.model.ParentNoteType
import uk.gov.justice.hmpps.casenotes.notes.AmendCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.repository.CaseNoteTypeRepository
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CaseNoteServiceTest {
  private val repository: OffenderCaseNoteRepository = mock()
  private val caseNoteTypeRepository: CaseNoteTypeRepository = mock()
  private val securityUserContext: SecurityUserContext = mock()
  private val externalApiService: ExternalApiService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private var caseNoteService: CaseNoteService = mock()
  private var entityManager: EntityManager = mock()
  private val requestAttributes: RequestAttributes = mock()

  @Captor
  lateinit var filterCaptor: ArgumentCaptor<OffenderCaseNoteFilter>

  @BeforeEach
  fun setUp() {
    caseNoteService = CaseNoteService(
      repository,
      caseNoteTypeRepository,
      securityUserContext,
      externalApiService,
      telemetryClient,
      entityManager,
    )
    RequestContextHolder.setRequestAttributes(requestAttributes)
    whenever(requestAttributes.getAttribute(CaseNoteRequestContext::class.simpleName!!, 0)).thenReturn(
      CaseNoteRequestContext("user", "author", "12345"),
    )
  }

  @Nested
  inner class CreateCaseNote {
    @Test
    fun `non sensitive explicit non system generated case note stored in NOMIS`() {
      whenever(
        caseNoteTypeRepository.findByParentTypeAndType(
          any<String>(),
          any<String>(),
        ),
      ).thenReturn(Optional.of(CaseNoteType.builder().syncToNomis(true).dpsUserSelectable(false).build()))
      val nomisCaseNote: NomisCaseNote = createNomisCaseNote()
      nomisCaseNote.source = "INT"

      val caseNoteCaptor = argumentCaptor<CreateCaseNoteRequest>()
      whenever(externalApiService.createCaseNote(any<String>(), caseNoteCaptor.capture())).thenReturn(nomisCaseNote)

      val caseNote = caseNoteService.createCaseNote(
        "12345",
        createCaseNoteRequest(systemGenerated = false),
      )

      val sent = caseNoteCaptor.firstValue
      assertThat(sent.systemGenerated).isEqualTo(false)

      assertThat(caseNote).usingRecursiveComparison()
        .ignoringFields(
          "authorUsername",
          "locationId",
          "text",
          "caseNoteId",
          "authorUserId",
          "eventId",
          "sensitive",
          "systemGenerated",
          "legacyId",
        )
        .isEqualTo(nomisCaseNote)
      assertThat(caseNote.text).isEqualTo("original")
      assertThat(caseNote.authorUserId).isEqualTo("23456")
      assertThat(caseNote.locationId).isEqualTo("agency")
      assertThat(caseNote.caseNoteId).isEqualTo("12345")
      assertThat(caseNote.eventId).isEqualTo(12345)
      assertThat(caseNote.systemGenerated).isFalse()
      assertThat(caseNote.legacyId).isEqualTo(12345)
      Mockito.verify(caseNoteTypeRepository).findByParentTypeAndType("ACP", "POS1")
    }

    @Test
    fun `non sensitive explicit system generated case note stored in NOMIS`() {
      whenever(
        caseNoteTypeRepository.findByParentTypeAndType(
          any<String>(),
          any<String>(),
        ),
      ).thenReturn(Optional.of(CaseNoteType.builder().syncToNomis(true).dpsUserSelectable(false).build()))
      val nomisCaseNote: NomisCaseNote = createNomisCaseNote()
      nomisCaseNote.caseNoteId = 6574632
      nomisCaseNote.source = "AUTO"

      val caseNoteCaptor = argumentCaptor<CreateCaseNoteRequest>()
      whenever(externalApiService.createCaseNote(any<String>(), caseNoteCaptor.capture())).thenReturn(nomisCaseNote)

      val caseNote = caseNoteService.createCaseNote(
        "12345",
        createCaseNoteRequest(systemGenerated = true),
      )

      val sent = caseNoteCaptor.firstValue
      assertThat(sent.systemGenerated).isEqualTo(true)

      assertThat(caseNote).usingRecursiveComparison()
        .ignoringFields(
          "authorUsername",
          "locationId",
          "text",
          "caseNoteId",
          "authorUserId",
          "eventId",
          "sensitive",
          "systemGenerated",
          "legacyId",
        )
        .isEqualTo(nomisCaseNote)
      assertThat(caseNote.text).isEqualTo("original")
      assertThat(caseNote.authorUserId).isEqualTo("23456")
      assertThat(caseNote.locationId).isEqualTo("agency")
      assertThat(caseNote.caseNoteId).isEqualTo("6574632")
      assertThat(caseNote.eventId).isEqualTo(6574632)
      assertThat(caseNote.systemGenerated).isTrue()
      assertThat(caseNote.legacyId).isEqualTo(6574632)
      Mockito.verify(caseNoteTypeRepository).findByParentTypeAndType("ACP", "POS1")
    }

    @Test
    fun `non sensitive system generated case note stored in NOMIS`() {
      whenever(
        caseNoteTypeRepository.findByParentTypeAndType(
          any<String>(),
          any<String>(),
        ),
      ).thenReturn(Optional.of(CaseNoteType.builder().syncToNomis(true).dpsUserSelectable(false).build()))
      val nomisCaseNote: NomisCaseNote = createNomisCaseNote()
      nomisCaseNote.source = "AUTO"

      val caseNoteCaptor = argumentCaptor<CreateCaseNoteRequest>()
      whenever(externalApiService.createCaseNote(any<String>(), caseNoteCaptor.capture())).thenReturn(nomisCaseNote)

      val caseNote = caseNoteService.createCaseNote("12345", createCaseNoteRequest())

      val sent = caseNoteCaptor.firstValue
      assertThat(sent.systemGenerated).isEqualTo(true)

      assertThat(caseNote).usingRecursiveComparison()
        .ignoringFields(
          "authorUsername",
          "locationId",
          "text",
          "caseNoteId",
          "authorUserId",
          "eventId",
          "sensitive",
          "systemGenerated",
          "legacyId",
        )
        .isEqualTo(nomisCaseNote)
      assertThat(caseNote.text).isEqualTo("original")
      assertThat(caseNote.authorUserId).isEqualTo("23456")
      assertThat(caseNote.locationId).isEqualTo("agency")
      assertThat(caseNote.caseNoteId).isEqualTo("12345")
      assertThat(caseNote.eventId).isEqualTo(12345)
      assertThat(caseNote.systemGenerated).isTrue()
      assertThat(caseNote.legacyId).isEqualTo(12345)
      Mockito.verify(caseNoteTypeRepository).findByParentTypeAndType("ACP", "POS1")
    }

    @Test
    fun `non sensitive and non system generated case note stored in NOMIS`() {
      whenever(
        caseNoteTypeRepository.findByParentTypeAndType(
          any<String>(),
          any<String>(),
        ),
      ).thenReturn(Optional.of(CaseNoteType.builder().syncToNomis(true).dpsUserSelectable(true).build()))
      val nomisCaseNote: NomisCaseNote = createNomisCaseNote()
      nomisCaseNote.source = "INST"

      val caseNoteCaptor = argumentCaptor<CreateCaseNoteRequest>()
      whenever(externalApiService.createCaseNote(any<String>(), caseNoteCaptor.capture())).thenReturn(nomisCaseNote)

      val caseNote = caseNoteService.createCaseNote("12345", createCaseNoteRequest())

      val sent = caseNoteCaptor.firstValue
      assertThat(sent.systemGenerated).isEqualTo(false)

      assertThat(caseNote).usingRecursiveComparison()
        .ignoringFields(
          "authorUsername",
          "locationId",
          "text",
          "caseNoteId",
          "authorUserId",
          "eventId",
          "sensitive",
          "systemGenerated",
          "legacyId",
        )
        .isEqualTo(nomisCaseNote)
      assertThat(caseNote.text).isEqualTo("original")
      assertThat(caseNote.authorUserId).isEqualTo("23456")
      assertThat(caseNote.locationId).isEqualTo("agency")
      assertThat(caseNote.caseNoteId).isEqualTo("12345")
      assertThat(caseNote.eventId).isEqualTo(12345)
      assertThat(caseNote.systemGenerated).isFalse()
      assertThat(caseNote.legacyId).isEqualTo(12345)
      Mockito.verify(caseNoteTypeRepository).findByParentTypeAndType("ACP", "POS1")
    }

    @Test
    fun `cannot create case note without appropriate role`() {
      whenever(
        caseNoteTypeRepository.findByParentTypeAndType(
          any<String>(),
          any<String>(),
        ),
      ).thenReturn(Optional.of(CaseNoteType.builder().build()))
      whenever(securityUserContext.isOverrideRole(any<String>(), any<String>())).thenReturn(false)

      assertThatThrownBy {
        caseNoteService.createCaseNote(
          "12345",
          createCaseNoteRequest(),
        )
      }.isInstanceOf(AccessDeniedException::class.java)

      Mockito.verify(securityUserContext).isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES")
    }

    @Nested
    inner class Sensitive {
      val caseNoteId = UUID.randomUUID()
      val now = LocalDateTime.now()
      val noteType: CaseNoteType = CaseNoteType.builder()
        .parentType(
          ParentNoteType.builder().type("someparent").description("description of parent")
            .createDateTime(LocalDateTime.MIN).build(),
        )
        .type("sometype")
        .description("description of some type")
        .sensitive(true)
        .createDateTime(LocalDateTime.MIN)
        .build()
      val defaultContext = CaseNoteRequestContext("someuser", "Some User", "userId", source = Source.DPS)

      @BeforeEach
      fun beforeEach() {
        whenever(caseNoteTypeRepository.findByParentTypeAndType(any(), any())).thenReturn(
          Optional.of(
            noteType,
          ),
        )
        whenever(securityUserContext.isOverrideRole(any<String>(), any<String>())).thenReturn(true)
        whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("someuser", "userId"))
        whenever(repository.saveAndFlush(any<OffenderCaseNote>())).thenAnswer { i ->
          val cn = (i.arguments[0] as OffenderCaseNote)
          cn.toBuilder().id(caseNoteId).createDateTime(now).eventId(1234)
            .amendments(
              cn.amendments.map { it.toBuilder().build() }
                .toSortedSet(AmendmentComparator()),
            )
            .build()
        }

        val request = MockHttpServletRequest()
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        RequestContextHolder.getRequestAttributes()!!
          .setAttribute(CaseNoteRequestContext::class.simpleName!!, defaultContext, 0)
      }

      @Test
      fun `sensitive user created case note stored outside NOMIS in dedicated database`() {
        RequestContextHolder.getRequestAttributes()!!
          .setAttribute(
            CaseNoteRequestContext::class.simpleName!!,
            defaultContext.copy(userDisplayName = "John Smith", userId = "4321"),
            0,
          )

        val createdNote: CaseNote = caseNoteService.createCaseNote(
          "A1234AA",
          createCaseNoteRequest(
            type = "someparent",
            subType = "sometype",
            occurrenceDateTime = now,
            text = "HELLO",
          ),
        )

        assertThat(createdNote).isEqualTo(
          CaseNote.builder()
            .caseNoteId(caseNoteId.toString())
            .offenderIdentifier("A1234AA")
            .sensitive(true)
            .type("someparent")
            .typeDescription("description of parent")
            .subType("sometype")
            .subTypeDescription("description of some type")
            .text("HELLO")
            .occurrenceDateTime(now)
            .authorName("John Smith")
            .authorUserId("4321")
            .source("OCNS")
            .systemGenerated(false)
            .amendments(emptyList())
            .creationDateTime(now)
            .eventId(1234)
            .locationId("MDI")
            .build(),
        )
      }

      @Test
      fun `sensitive system generated case note stored outside NOMIS in dedicated database`() {
        RequestContextHolder.getRequestAttributes()!!
          .setAttribute(
            CaseNoteRequestContext::class.simpleName!!,
            defaultContext.copy(userDisplayName = "John Smith", userId = "4321"),
            0,
          )

        val createdNote: CaseNote = caseNoteService.createCaseNote(
          "A1234AA",
          createCaseNoteRequest(
            type = "someparent",
            subType = "sometype",
            occurrenceDateTime = now,
            text = "HELLO",
            systemGenerated = true,
          ),
        )

        assertThat(createdNote).isEqualTo(
          CaseNote.builder()
            .caseNoteId(caseNoteId.toString())
            .offenderIdentifier("A1234AA")
            .sensitive(true)
            .type("someparent")
            .typeDescription("description of parent")
            .subType("sometype")
            .subTypeDescription("description of some type")
            .text("HELLO")
            .occurrenceDateTime(now)
            .authorName("John Smith")
            .authorUserId("4321")
            .source("OCNS")
            .amendments(emptyList())
            .creationDateTime(now)
            .eventId(1234)
            .locationId("MDI")
            .systemGenerated(true)
            .build(),
        )
      }

      @Test
      fun `sensitive case note defaults author details to username where user details not returned`() {
        RequestContextHolder.getRequestAttributes()!!
          .setAttribute(
            CaseNoteRequestContext::class.simpleName!!,
            defaultContext.copy(userDisplayName = "someuser", userId = "someuser"),
            0,
          )

        val createdNote: CaseNote = caseNoteService.createCaseNote(
          "A1234AA",
          createCaseNoteRequest(
            type = "someparent",
            subType = "sometype",
            occurrenceDateTime = now,
            text = "HELLO",
          ),
        )

        assertThat(createdNote.authorName).isEqualTo("someuser")
        assertThat(createdNote.authorUserId).isEqualTo("someuser")
      }

      @Test
      fun `sensitive case note defaults author name to username where user name not returned`() {
        RequestContextHolder.getRequestAttributes()!!
          .setAttribute(
            CaseNoteRequestContext::class.simpleName!!,
            defaultContext.copy(userDisplayName = "someuser", userId = "4321"),
            0,
          )

        val createdNote: CaseNote = caseNoteService.createCaseNote(
          "A1234AA",
          createCaseNoteRequest(
            type = "someparent",
            subType = "sometype",
            occurrenceDateTime = now,
            text = "HELLO",
          ),
        )

        assertThat(createdNote.authorName).isEqualTo("someuser")
      }

      @Test
      fun `sensitive case note defaults author name to username where user id not returned`() {
        RequestContextHolder.getRequestAttributes()!!
          .setAttribute(
            CaseNoteRequestContext::class.simpleName!!,
            defaultContext.copy(userDisplayName = "someuser", userId = "someuser"),
            0,
          )

        val createdNote: CaseNote = caseNoteService.createCaseNote(
          "A1234AA",
          createCaseNoteRequest(
            type = "someparent",
            subType = "sometype",
            occurrenceDateTime = now,
            text = "HELLO",
          ),
        )

        assertThat(createdNote.authorUserId).isEqualTo("someuser")
      }
    }
  }

  @Test
  fun caseNote_noAddRole() {
    val noteType: CaseNoteType =
      CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
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
    val noteType: CaseNoteType =
      CaseNoteType.builder().type("sometype").description("Type Description")
        .parentType(ParentNoteType.builder().type("parenttype").description("Parent Type Description").build())
        .createDateTime(LocalDateTime.MIN)
        .build()
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
        "systemGenerated",
        "legacyId",
      )
      .isEqualTo(nomisCaseNote)
    assertThat(caseNote.text).isEqualTo("original")
    assertThat(caseNote.authorUserId).isEqualTo("23456")
    assertThat(caseNote.locationId).isEqualTo("agency")
    assertThat(caseNote.caseNoteId).isEqualTo("12345")
    assertThat(caseNote.eventId).isEqualTo(12345)
    assertThat(caseNote.legacyId).isEqualTo(12345)
    assertThat(caseNote.systemGenerated).isEqualTo(false)
  }

  @Test
  fun caseNoteWithAmendment_callElite2() {
    val nomisCaseNote: NomisCaseNote = createNomisCaseNote()
    nomisCaseNote.amendments = listOf(
      NomisCaseNoteAmendment.builder()
        .additionalNoteText("additional details")
        .authorUsername("AAA11B")
        .authorName("A. Name")
        .authorUserId("AU1234TH")
        .creationDateTime(LocalDateTime.parse("2019-03-24T11:22"))
        .caseNoteAmendmentId(NomisIdGenerator.newId())
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
        "systemGenerated",
        "legacyId",
      )
      .isEqualTo(nomisCaseNote)
    assertThat(caseNote.text).isEqualTo("original")
    assertThat(caseNote.authorUserId).isEqualTo("23456")
    assertThat(caseNote.locationId).isEqualTo("agency")
    assertThat(caseNote.caseNoteId).isEqualTo("12345")
    assertThat(caseNote.eventId).isEqualTo(12345)
    assertThat(caseNote.legacyId).isEqualTo(12345)
    assertThat(caseNote.amendments.get(0).additionalNoteText).isEqualTo("additional details")
    assertThat(caseNote.amendments.get(0).authorUserName).isEqualTo("AAA11B")
    assertThat(caseNote.amendments.get(0).creationDateTime).isEqualTo("2019-03-24T11:22")
  }

  @Nested
  inner class AmendCaseNoteRequest {
    @Test
    fun `amend non sensitive case note`() {
      val nomisCaseNote: NomisCaseNote = createNomisCaseNote()
      whenever(externalApiService.amendOffenderCaseNote(any<String>(), any<Long>(), any())).thenReturn(nomisCaseNote)

      val caseNote: CaseNote = caseNoteService.amendCaseNote(
        "12345",
        "21455",
        AmendCaseNoteRequest("text"),
      )

      assertThat(caseNote).usingRecursiveComparison()
        .ignoringFields(
          "authorUsername",
          "locationId",
          "text",
          "caseNoteId",
          "authorUserId",
          "eventId",
          "sensitive",
          "systemGenerated",
          "legacyId",
        )
        .isEqualTo(nomisCaseNote)

      assertThat(caseNote.text).isEqualTo("original")
      assertThat(caseNote.authorUserId).isEqualTo("23456")
      assertThat(caseNote.locationId).isEqualTo("agency")
      assertThat(caseNote.caseNoteId).isEqualTo("12345")
      assertThat(caseNote.eventId).isEqualTo(12345)
      assertThat(caseNote.legacyId).isEqualTo(12345)
    }

    @Test
    fun `cannot amend case note without required roles`() {
      val noteType: CaseNoteType =
        CaseNoteType.builder().type("sometype").parentType(ParentNoteType.builder().build()).build()
      val offenderCaseNote: OffenderCaseNote = createOffenderCaseNote(noteType)
      whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))

      assertThatThrownBy {
        caseNoteService.amendCaseNote(
          "12345",
          UUID.randomUUID().toString(),
          AmendCaseNoteRequest("text"),
        )
      }
        .isInstanceOf(AccessDeniedException::class.java)

      Mockito.verify(securityUserContext).isOverrideRole("POM", "ADD_SENSITIVE_CASE_NOTES")
    }

    @Test
    fun `cannot amend case note with unknown id`() {
      val caseNoteIdentifier: String = UUID.randomUUID().toString()

      assertThatThrownBy {
        caseNoteService.amendCaseNote(
          "12345",
          caseNoteIdentifier,
          AmendCaseNoteRequest("text"),
        )
      }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage(String.format("Resource with id [%s] not found.", caseNoteIdentifier))
    }

    @Test
    fun `cannot amend case note when wrong offender identifier supplied`() {
      val noteType: CaseNoteType =
        CaseNoteType.builder().type("sometype").restrictedUse(false).parentType(ParentNoteType.builder().build())
          .build()
      val offenderCaseNote: OffenderCaseNote = createOffenderCaseNote(noteType)
      whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))

      assertThatThrownBy {
        caseNoteService.amendCaseNote(
          "12345",
          UUID.randomUUID().toString(),
          AmendCaseNoteRequest("text"),
        )
      }
        .isInstanceOf(EntityNotFoundException::class.java).hasMessage("Resource with id [12345] not found.")
    }

    @Nested
    inner class Sensitive {
      val caseNoteIdentifier = UUID.randomUUID()

      @BeforeEach
      fun beforeEach() {
        val noteType: CaseNoteType =
          CaseNoteType.builder().type("sometype").description("Type Description")
            .parentType(ParentNoteType.builder().type("ParentType").description("Parent Type Description").build())
            .createDateTime(LocalDateTime.MIN)
            .restrictedUse(false)
            .build()
        val offenderCaseNote: OffenderCaseNote = createOffenderCaseNote(noteType)
        whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))
        whenever(repository.save(any<OffenderCaseNote>())).thenAnswer { i ->
          val cn = (i.arguments[0] as OffenderCaseNote)
          cn.toBuilder()
            .amendments(
              cn.amendments.map { it.toBuilder().build() }
                .toSortedSet(AmendmentComparator()),
            )
            .build()
        }
        RequestContextHolder.setRequestAttributes(requestAttributes)
        whenever(requestAttributes.getAttribute(CaseNoteRequestContext::class.simpleName!!, 0)).thenReturn(
          CaseNoteRequestContext("user", "author", "12345"),
        )
      }

      @Test
      fun `can amend sensitive case note`() {
        val caseNote: CaseNote =
          caseNoteService.amendCaseNote(
            "A1234AC",
            caseNoteIdentifier.toString(),
            AmendCaseNoteRequest("text"),
          )

        assertThat(caseNote.amendments).hasSize(1)

        val expected = CaseNoteAmendment(
          creationDateTime = LocalDateTime.now(),
          authorUserName = "user",
          authorName = "author",
          authorUserId = "12345",
          additionalNoteText = "text",
        )

        assertThat(caseNote.amendments[0]).usingRecursiveComparison()
          .comparingOnlyFields(
            "additionalNoteText",
            "authorName",
            "authorUserName",
            "authorUserId",
          ).isEqualTo(expected)
      }

      @Test
      fun `sensitive case note amendment defaults author details to username where user details not returned`() {
        whenever(requestAttributes.getAttribute(CaseNoteRequestContext::class.simpleName!!, 0)).thenReturn(
          CaseNoteRequestContext("user"),
        )
        val caseNote: CaseNote =
          caseNoteService.amendCaseNote(
            "A1234AC",
            caseNoteIdentifier.toString(),
            AmendCaseNoteRequest("text"),
          )

        assertThat(caseNote.amendments[0].authorName).isEqualTo("user")
        assertThat(caseNote.amendments[0].authorUserId).isEqualTo("user")
      }

      @Test
      fun `sensitive case note amendment defaults author name to username where user name not returned`() {
        whenever(requestAttributes.getAttribute(CaseNoteRequestContext::class.simpleName!!, 0)).thenReturn(
          CaseNoteRequestContext("user"),
        )

        val caseNote: CaseNote =
          caseNoteService.amendCaseNote(
            "A1234AC",
            caseNoteIdentifier.toString(),
            AmendCaseNoteRequest("text"),
          )

        assertThat(caseNote.amendments[0].authorName).isEqualTo("user")
      }

      @Test
      fun `sensitive case note amendment defaults author name to username where user id not returned`() {
        whenever(requestAttributes.getAttribute(CaseNoteRequestContext::class.simpleName!!, 0)).thenReturn(
          CaseNoteRequestContext("user"),
        )

        val caseNote: CaseNote =
          caseNoteService.amendCaseNote(
            "A1234AC",
            caseNoteIdentifier.toString(),
            AmendCaseNoteRequest("text"),
          )

        assertThat(caseNote.amendments[0].authorUserId).isEqualTo("user")
      }
    }
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
    Mockito.verify(telemetryClient)
      .trackEvent("OffenderDelete", mapOf("offenderNo" to "A1234AC", "count" to "3"), null)
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

    Mockito.verify(telemetryClient).trackEvent(
      "SecureCaseNoteSoftDelete",
      mapOf("userName" to "user", "offenderId" to "A1234AC", "case note id" to offenderCaseNoteId.toString()),
      null,
    )
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
  fun getCaseNotes_callPrisonerApi() {
    val nomisCaseNote = createNomisCaseNote("someType", "someSubType")
    val pageable = PageRequest.of(0, 10, Direction.DESC, "occurrenceDateTime")
    val apiResponseList = listOf(nomisCaseNote)
    val repositoryResponseList = listOf<OffenderCaseNote>()
    whenever(repository.findAll(ArgumentMatchers.any<Specification<OffenderCaseNote>>())).thenReturn(
      repositoryResponseList,
    )
    whenever(externalApiService.getOffenderCaseNotes(anyString(), any(), any())).thenReturn(
      PageImpl(
        apiResponseList,
        pageable,
        1,
      ),
    )
    whenever(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(true)

    val filter = CaseNoteFilter("someType", "someSubType")
    val caseNotes = caseNoteService.getCaseNotes("12345", filter, pageable).content
    assertThat(caseNotes.size).isEqualTo(1)
    assert(caseNotes.stream().allMatch { x: CaseNote -> x.type == "someType" && x.subType == "someSubType" })
    assert(caseNotes.stream().anyMatch { x: CaseNote -> x.text == "original" })
  }

  @Test
  fun getCaseNotes_GetFromRepositoryAndPrisonApi() {
    val noteType =
      CaseNoteType.builder()
        .type("someSubType").description("Type Description")
        .parentType(ParentNoteType.builder().type("someType").description("Parent Type Description").build())
        .build()
    val offenderCaseNote = createOffenderCaseNote(noteType)
    val pageable = PageRequest.of(0, 10, Direction.DESC, "occurrenceDateTime")
    whenever(repository.findAll(ArgumentMatchers.any<Specification<OffenderCaseNote>>())).thenReturn(
      listOf(
        offenderCaseNote,
      ),
    )
    val apiResponseList = listOf(createNomisCaseNote("someType", "someSubType"))
    whenever(externalApiService.getOffenderCaseNotes(anyString(), any(), any())).thenReturn(
      PageImpl(
        apiResponseList,
        pageable,
        1,
      ),
    )
    whenever(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(true)
    val filter = CaseNoteFilter("someType", "someSUbType")

    val caseNotes = caseNoteService.getCaseNotes("12345", filter, pageable).content

    assertThat(caseNotes.size).isEqualTo(2)
    assert(caseNotes.stream().allMatch { x: CaseNote -> x.type == "someType" && x.subType == "someSubType" })
    assert(caseNotes.stream().anyMatch { x: CaseNote -> x.text == "HELLO" })
    assert(caseNotes.stream().anyMatch { x: CaseNote -> x.text == "original" })
  }

  @Test
  fun `getCaseNotes includes sensitive case notes by default when presented with client credentials token`() {
    whenever(repository.findAll(ArgumentMatchers.any<Specification<OffenderCaseNote>>())).thenReturn(emptyList())
    whenever(externalApiService.getOffenderCaseNotes(anyString(), any(), any())).thenReturn(Page.empty())
    whenever(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(true)

    val filter = CaseNoteFilter()
    val pageable = PageRequest.of(0, 10, Direction.DESC, "occurrenceDateTime")
    caseNoteService.getCaseNotes("12345", filter, pageable).content

    verify(repository).findAll(filterCaptor.capture())
    assertThat(filterCaptor.value.excludeSensitive).isFalse()
  }

  @Test
  fun `getCaseNotes excludes sensitive case notes when presented with client credentials token and includeSensitive is false`() {
    whenever(repository.findAll(ArgumentMatchers.any<Specification<OffenderCaseNote>>())).thenReturn(emptyList())
    whenever(externalApiService.getOffenderCaseNotes(anyString(), any(), any())).thenReturn(Page.empty())
    whenever(securityUserContext.isOverrideRole(anyString(), anyString(), anyString())).thenReturn(true)

    val filter = CaseNoteFilter(includeSensitive = false)
    val pageable = PageRequest.of(0, 10, Direction.DESC, "occurrenceDateTime")
    caseNoteService.getCaseNotes("12345", filter, pageable).content

    verify(repository).findAll(filterCaptor.capture())
    assertThat(filterCaptor.value.excludeSensitive).isTrue()
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
      .eventId(1234)
      .createDateTime(LocalDateTime.now().minusDays(7))
      .build()
  }

  private fun createOffenderCaseNoteAmendment(caseNoteType: CaseNoteType): Optional<OffenderCaseNoteAmendment> {
    val amendment = OffenderCaseNoteAmendment
      .builder()
      .caseNote(createOffenderCaseNote(caseNoteType))
      .noteText("A")
      .authorName("some user")
      .build()

    return Optional.of(amendment)
  }

  private fun createCaseNoteRequest(
    locationId: String? = "MDI",
    type: String = "ACP",
    subType: String = "POS1",
    occurrenceDateTime: LocalDateTime = LocalDateTime.now(),
    text: String = "Some text for the note",
    systemGenerated: Boolean? = null,
  ) = CreateCaseNoteRequest(locationId, type, subType, occurrenceDateTime, text, systemGenerated)
}
