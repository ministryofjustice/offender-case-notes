package uk.gov.justice.hmpps.casenotes.services

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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
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
import uk.gov.justice.hmpps.casenotes.legacy.dto.NomisCaseNote
import uk.gov.justice.hmpps.casenotes.legacy.dto.NomisCaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.legacy.filters.OffenderCaseNoteFilter
import uk.gov.justice.hmpps.casenotes.legacy.model.CaseNoteSubType
import uk.gov.justice.hmpps.casenotes.legacy.model.CaseNoteType
import uk.gov.justice.hmpps.casenotes.legacy.model.OffenderCaseNote
import uk.gov.justice.hmpps.casenotes.legacy.model.OffenderCaseNote.AmendmentComparator
import uk.gov.justice.hmpps.casenotes.legacy.repository.CaseNoteSubTypeRepository
import uk.gov.justice.hmpps.casenotes.legacy.repository.OffenderCaseNoteRepository
import uk.gov.justice.hmpps.casenotes.legacy.service.CaseNoteService
import uk.gov.justice.hmpps.casenotes.legacy.service.EntityNotFoundException
import uk.gov.justice.hmpps.casenotes.legacy.service.ExternalApiService
import uk.gov.justice.hmpps.casenotes.notes.AmendCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteAmendment
import uk.gov.justice.hmpps.casenotes.notes.CaseNoteFilter
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.utils.NomisIdGenerator
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CaseNoteServiceTest {
  private val repository: OffenderCaseNoteRepository = mock()
  private val caseNoteSubTypeRepository: CaseNoteSubTypeRepository = mock()
  private val securityUserContext: SecurityUserContext = mock()
  private val externalApiService: ExternalApiService = mock()
  private var caseNoteService: CaseNoteService = mock()
  private val requestAttributes: RequestAttributes = mock()
  private val eventPublisher: ApplicationEventPublisher = mock()

  @Captor
  lateinit var filterCaptor: ArgumentCaptor<OffenderCaseNoteFilter>

  @BeforeEach
  fun setUp() {
    caseNoteService = CaseNoteService(
      repository,
      caseNoteSubTypeRepository,
      securityUserContext,
      externalApiService,
      eventPublisher,
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
        caseNoteSubTypeRepository.findByParentTypeAndType(
          any<String>(),
          any<String>(),
        ),
      ).thenReturn(Optional.of(CaseNoteSubType.builder().syncToNomis(true).dpsUserSelectable(false).build()))
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
          "id",
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
      assertThat(caseNote.id).isEqualTo("12345")
      assertThat(caseNote.eventId).isEqualTo(12345)
      assertThat(caseNote.systemGenerated).isFalse()
      assertThat(caseNote.legacyId).isEqualTo(12345)
      Mockito.verify(caseNoteSubTypeRepository).findByParentTypeAndType("ACP", "POS1")
    }

    @Test
    fun `non sensitive explicit system generated case note stored in NOMIS`() {
      whenever(
        caseNoteSubTypeRepository.findByParentTypeAndType(
          any<String>(),
          any<String>(),
        ),
      ).thenReturn(Optional.of(CaseNoteSubType.builder().syncToNomis(true).dpsUserSelectable(false).build()))
      val nomisCaseNote: NomisCaseNote = createNomisCaseNote()
      nomisCaseNote.id = 6574632
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
          "id",
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
      assertThat(caseNote.id).isEqualTo("6574632")
      assertThat(caseNote.eventId).isEqualTo(6574632)
      assertThat(caseNote.systemGenerated).isTrue()
      assertThat(caseNote.legacyId).isEqualTo(6574632)
      Mockito.verify(caseNoteSubTypeRepository).findByParentTypeAndType("ACP", "POS1")
    }

    @Test
    fun `non sensitive system generated case note stored in NOMIS`() {
      whenever(
        caseNoteSubTypeRepository.findByParentTypeAndType(
          any<String>(),
          any<String>(),
        ),
      ).thenReturn(Optional.of(CaseNoteSubType.builder().syncToNomis(true).dpsUserSelectable(false).build()))
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
          "id",
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
      assertThat(caseNote.id).isEqualTo("12345")
      assertThat(caseNote.eventId).isEqualTo(12345)
      assertThat(caseNote.systemGenerated).isTrue()
      assertThat(caseNote.legacyId).isEqualTo(12345)
      Mockito.verify(caseNoteSubTypeRepository).findByParentTypeAndType("ACP", "POS1")
    }

    @Test
    fun `non sensitive and non system generated case note stored in NOMIS`() {
      whenever(
        caseNoteSubTypeRepository.findByParentTypeAndType(
          any<String>(),
          any<String>(),
        ),
      ).thenReturn(Optional.of(CaseNoteSubType.builder().syncToNomis(true).dpsUserSelectable(true).build()))
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
          "id",
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
      assertThat(caseNote.id).isEqualTo("12345")
      assertThat(caseNote.eventId).isEqualTo(12345)
      assertThat(caseNote.systemGenerated).isFalse()
      assertThat(caseNote.legacyId).isEqualTo(12345)
      Mockito.verify(caseNoteSubTypeRepository).findByParentTypeAndType("ACP", "POS1")
    }

    @Test
    fun `cannot create case note without appropriate role`() {
      whenever(
        caseNoteSubTypeRepository.findByParentTypeAndType(
          any<String>(),
          any<String>(),
        ),
      ).thenReturn(Optional.of(CaseNoteSubType.builder().build()))
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
      val noteType: CaseNoteSubType = CaseNoteSubType.builder()
        .type(
          CaseNoteType.builder().code("someparent").description("description of parent").build(),
        )
        .code("sometype")
        .description("description of some type")
        .sensitive(true)
        .build()
      val defaultContext = CaseNoteRequestContext("someuser", "Some User", "userId", source = Source.DPS)

      @BeforeEach
      fun beforeEach() {
        whenever(caseNoteSubTypeRepository.findByParentTypeAndType(any(), any())).thenReturn(
          Optional.of(
            noteType,
          ),
        )
        whenever(securityUserContext.isOverrideRole(any<String>(), any<String>())).thenReturn(true)
        whenever(securityUserContext.getCurrentUser()).thenReturn(UserIdUser("someuser", "userId"))
        whenever(repository.saveAndFlush(any<OffenderCaseNote>())).thenAnswer { i ->
          val cn = (i.arguments[0] as OffenderCaseNote)
          cn.toBuilder().id(caseNoteId).createdAt(now).legacyId(1234)
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
            defaultContext.copy(userDisplayName = "John Smith", userId = "4321", username = "JOHN_SMITH"),
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
            .id(caseNoteId.toString())
            .personIdentifier("A1234AA")
            .sensitive(true)
            .type("someparent")
            .typeDescription("description of parent")
            .subType("sometype")
            .subTypeDescription("description of some type")
            .text("HELLO")
            .occurredAt(now)
            .authorName("John Smith")
            .authorUserId("4321")
            .authorUsername("JOHN_SMITH")
            .source("OCNS")
            .systemGenerated(false)
            .amendments(emptyList())
            .createdAt(now)
            .eventId(1234)
            .legacyId(1234)
            .locationId("MDI")
            .build(),
        )
      }

      @Test
      fun `sensitive system generated case note stored outside NOMIS in dedicated database`() {
        RequestContextHolder.getRequestAttributes()!!
          .setAttribute(
            CaseNoteRequestContext::class.simpleName!!,
            defaultContext.copy(userDisplayName = "John Smith", userId = "4321", username = "JOHN_SMITH"),
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
            .id(caseNoteId.toString())
            .personIdentifier("A1234AA")
            .sensitive(true)
            .type("someparent")
            .typeDescription("description of parent")
            .subType("sometype")
            .subTypeDescription("description of some type")
            .text("HELLO")
            .occurredAt(now)
            .authorName("John Smith")
            .authorUserId("4321")
            .authorUsername("JOHN_SMITH")
            .source("OCNS")
            .amendments(emptyList())
            .createdAt(now)
            .eventId(1234)
            .legacyId(1234)
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
    val noteType: CaseNoteSubType =
      CaseNoteSubType.builder().code("sometype").type(
        CaseNoteType.builder().build(),
      ).build()
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
    val noteType: CaseNoteSubType =
      CaseNoteSubType.builder().code("sometype").description("Type Description")
        .type(CaseNoteType.builder().code("parenttype").description("Parent Type Description").build())
        .build()
    val offenderCaseNote: OffenderCaseNote = createOffenderCaseNote(noteType)
    whenever(repository.findById(any())).thenReturn(Optional.of(offenderCaseNote))
    whenever(securityUserContext.isOverrideRole(any<String>(), any<String>(), any<String>())).thenReturn(true)

    val caseNote: CaseNote = caseNoteService.getCaseNote("12345", UUID.randomUUID().toString())
    assertThat(caseNote).usingRecursiveComparison()
      .ignoringFields(
        "id",
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
        "eventId",
        "occurrenceDateTime",
        "offenderIdentifier",
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
        "id",
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
    assertThat(caseNote.id).isEqualTo("12345")
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
        "id",
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
    assertThat(caseNote.id).isEqualTo("12345")
    assertThat(caseNote.eventId).isEqualTo(12345)
    assertThat(caseNote.legacyId).isEqualTo(12345)
    assertThat(caseNote.amendments.get(0).additionalNoteText).isEqualTo("additional details")
    assertThat(caseNote.amendments.get(0).authorUserName).isEqualTo("AAA11B")
    assertThat(caseNote.amendments.get(0).createdAt).isEqualTo("2019-03-24T11:22")
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
          "id",
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
      assertThat(caseNote.id).isEqualTo("12345")
      assertThat(caseNote.eventId).isEqualTo(12345)
      assertThat(caseNote.legacyId).isEqualTo(12345)
    }

    @Test
    fun `cannot amend case note without required roles`() {
      val noteType: CaseNoteSubType =
        CaseNoteSubType.builder().code("sometype").type(
          CaseNoteType.builder().build(),
        ).build()
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
      val noteType: CaseNoteSubType =
        CaseNoteSubType.builder().code("sometype").restrictedUse(false).type(
          CaseNoteType.builder().build(),
        )
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
        val noteType: CaseNoteSubType =
          CaseNoteSubType.builder().code("sometype").description("Type Description")
            .type(CaseNoteType.builder().code("ParentType").description("Parent Type Description").build())
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
          createdAt = LocalDateTime.now(),
          authorUserName = "user",
          authorName = "author",
          authorUserId = "12345",
          additionalNoteText = "text",
          UUID.randomUUID(),
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
      CaseNoteSubType.builder()
        .code("someSubType").description("Type Description")
        .type(CaseNoteType.builder().code("someType").description("Parent Type Description").build())
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

  private fun createNomisCaseNote(): NomisCaseNote = NomisCaseNote.builder()
    .agencyId("agency")
    .authorUsername("SOME1")
    .authorName("somebody")
    .id(12345)
    .createdAt(LocalDateTime.parse("2019-03-23T11:22"))
    .occurredAt(LocalDateTime.parse("2019-04-16T10:42"))
    .originalNoteText("original")
    .source("WHERE")
    .staffId(23456L)
    .subType("SUB")
    .subTypeDescription("Sub desc")
    .text("new text")
    .type("type")
    .typeDescription("Type desc")
    .personIdentifier("12345")
    .build()

  private fun createNomisCaseNote(type: String, subType: String): NomisCaseNote = NomisCaseNote.builder()
    .agencyId("agency")
    .authorUsername("SOME1")
    .authorName("somebody")
    .id(12345)
    .createdAt(LocalDateTime.parse("2019-03-23T11:22"))
    .occurredAt(LocalDateTime.parse("2019-04-16T10:42"))
    .originalNoteText("original")
    .source("WHERE")
    .staffId(23456L)
    .subType(subType)
    .subTypeDescription("Sub desc")
    .text("new text")
    .type(type)
    .typeDescription("Type desc")
    .personIdentifier("12345")
    .build()

  private fun createOffenderCaseNote(caseNoteSubType: CaseNoteSubType): OffenderCaseNote = OffenderCaseNote.builder()
    .id(UUID.randomUUID())
    .occurredAt(LocalDateTime.now())
    .locationId("MDI")
    .authorUsername("USER2")
    .authorUserId("some user")
    .authorName("John Smith")
    .personIdentifier("A1234AC")
    .subType(caseNoteSubType)
    .text("HELLO")
    .legacyId(1234)
    .createdAt(LocalDateTime.now().minusDays(7))
    .build()

  private fun createCaseNoteRequest(
    locationId: String? = "MDI",
    type: String = "ACP",
    subType: String = "POS1",
    occurrenceDateTime: LocalDateTime = LocalDateTime.now(),
    text: String = "Some text for the note",
    systemGenerated: Boolean? = null,
  ) = CreateCaseNoteRequest(locationId, type, subType, occurrenceDateTime, text, systemGenerated)
}
