@file:Suppress("ClassName")

package uk.gov.justice.hmpps.casenotes.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.hmpps.casenotes.dto.BookingIdentifier
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter
import uk.gov.justice.hmpps.casenotes.dto.NomisCaseNote
import uk.gov.justice.hmpps.casenotes.dto.OffenderBooking
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNote
import uk.gov.justice.hmpps.casenotes.dto.UserDetails
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest
import java.time.LocalDateTime
import java.util.Optional

class ExternalApiServiceTest {
  private val responseSpecMock: ResponseSpec = mock()
  private val prisonApiWebClient: WebClient = mock()
  private val authWebClient: WebClient = mock()
  private val prisonApiClientCredentialsWebClient: WebClient = mock()
  private val requestHeadersUriSpec: RequestHeadersUriSpec<*> = mock()
  private val requestHeadersSpec: RequestHeadersSpec<*> = mock()
  private val requestBodyUriSpec: RequestBodyUriSpec = mock()
  private val requestBodySpec: RequestBodySpec = mock()

  private val externalApiService: ExternalApiService = ExternalApiService(
    prisonApiWebClient,
    authWebClient,
    prisonApiClientCredentialsWebClient,
  )

  @BeforeEach
  fun setup() {
    whenever(prisonApiWebClient.get()).thenReturn(requestHeadersUriSpec)
    whenever(authWebClient.get()).thenReturn(requestHeadersUriSpec)
    whenever(prisonApiClientCredentialsWebClient.get()).thenReturn(requestHeadersUriSpec)
    whenever(requestHeadersUriSpec.uri(any<String>())).thenReturn(requestHeadersSpec)
    whenever(requestHeadersUriSpec.uri(any<String>(), any<Any>())).thenReturn(requestHeadersSpec)
    whenever(requestBodyUriSpec.uri(any<String>(), any<Any>())).thenReturn(requestBodySpec)
    whenever(requestHeadersUriSpec.uri(any<String>(), any<Any>(), any<Any>())).thenReturn(requestHeadersSpec)
    whenever(
      requestHeadersUriSpec.uri(
        any<String>(),
        any<Any>(),
        any<Any>(),
        any<Any>(),
      ),
    ).thenReturn(requestHeadersSpec)
    whenever(requestBodyUriSpec.uri(any<String>(), any<Any>(), any<Any>())).thenReturn(requestBodySpec)
    whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpecMock)
    whenever(requestBodySpec.retrieve()).thenReturn(responseSpecMock)
  }

  @Nested
  inner class getMergedIdentifiersByBookingId {
    @Test
    fun `test calls Prison API`() {
      val result = listOf(BookingIdentifier(type = "MERGED", value = "AB12345C"))
      whenever(responseSpecMock.bodyToFlux(any<ParameterizedTypeReference<BookingIdentifier>>())).thenReturn(
        Flux.fromIterable(result),
      )
      assertThat(externalApiService.getMergedIdentifiersByBookingId(12345)).containsExactlyElementsOf(result)

      verify(prisonApiClientCredentialsWebClient).get()
      verify(requestHeadersUriSpec).uri("/api/bookings/{bookingId}/identifiers?type={type}", 12345L, "MERGED")
    }
  }

  @Nested
  inner class getBooking {
    @Test
    fun `test calls Prison API`() {
      val result = OffenderBooking(bookingId = 12345L, offenderNo = "AA123B", agencyId = "LSI")
      whenever(responseSpecMock.bodyToMono(any<ParameterizedTypeReference<OffenderBooking>>())).thenReturn(
        Mono.just(result),
      )
      assertThat(externalApiService.getBooking(12345)).isSameAs(result)

      verify(prisonApiClientCredentialsWebClient).get()
      verify(requestHeadersUriSpec).uri("/api/bookings/{bookingId}?basicInfo=true", 12345L)
    }
  }

  @Nested
  inner class getUserDetails {
    @Test
    fun `test calls HMPPS Auth`() {
      val userDetails = UserDetails(name = "Joe")
      whenever(responseSpecMock.bodyToMono(any<ParameterizedTypeReference<UserDetails>>()))
        .thenReturn(Mono.just(userDetails))
      assertThat(externalApiService.getUserDetails("user")).isEqualTo(Optional.of(userDetails))

      verify(authWebClient).get()
      verify(requestHeadersUriSpec).uri("/api/user/{username}", "user")
    }

    @Test
    fun `test calls HMPPS Auth and returns empty if no response`() {
      whenever(responseSpecMock.bodyToMono(any<ParameterizedTypeReference<UserDetails>>())).thenReturn(Mono.empty())
      assertThat(externalApiService.getUserDetails("user")).isEmpty

      verify(authWebClient).get()
      verify(requestHeadersUriSpec).uri("/api/user/{username}", "user")
    }
  }

  @Nested
  inner class getOffenderLocation {
    @Test
    fun `test calls Prison API`() {
      whenever(responseSpecMock.bodyToMono(any<ParameterizedTypeReference<OffenderBooking>>())).thenReturn(
        Mono.just(OffenderBooking(agencyId = "MDI", bookingId = 12345L, offenderNo = "AA123B")),
      )
      assertThat(externalApiService.getOffenderLocation("AA123B")).isEqualTo("MDI")

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri("/api/bookings/offenderNo/{offenderNo}", "AA123B")
    }
  }

  @Nested
  inner class getOffenderCaseNotes {
    @Test
    fun `test calls Prison API for first page with correct page size`() {
      val content = listOf(NomisCaseNote())
      val pageable = Pageable.ofSize(10)
      whenever(responseSpecMock.toEntity(any<ParameterizedTypeReference<*>>())).thenReturn(
        Mono.just(ResponseEntity(RestResponsePage(content, pageable, 12), HttpStatus.OK)),
      )
      val response = externalApiService.getOffenderCaseNotes("AA123B", CaseNoteFilter(), pageable)
      assertThat(response.totalElements).isEqualTo(12)
      assertThat(response.content).isEqualTo(content)

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri(
        check {
          val components = UriComponentsBuilder.fromUriString(it).build()
          assertThat(components.path).isEqualTo("/api/offenders/{offenderIdentifier}/case-notes/v2")
          assertThat(components.queryParams).containsExactlyInAnyOrderEntriesOf(
            mapOf("size" to listOf("10"), "page" to listOf("0")),
          )
        },
        eq("AA123B"),
      )
    }

    @Test
    fun `test calls Prison API for correct page with correct page size`() {
      val content = listOf(NomisCaseNote())
      val pageable = PageRequest.of(5, 20)
      whenever(responseSpecMock.toEntity(any<ParameterizedTypeReference<*>>())).thenReturn(
        Mono.just(ResponseEntity(RestResponsePage(content, pageable, 12), HttpStatus.OK)),
      )
      val response = externalApiService.getOffenderCaseNotes("AA123B", CaseNoteFilter(), pageable)
      assertThat(response.totalElements).isEqualTo(101)
      assertThat(response.content).isEqualTo(content)

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri(
        check {
          val components = UriComponentsBuilder.fromUriString(it).build()
          assertThat(components.path).isEqualTo("/api/offenders/{offenderIdentifier}/case-notes/v2")
          assertThat(components.queryParams).containsExactlyInAnyOrderEntriesOf(
            mapOf("size" to listOf("20"), "page" to listOf("5")),
          )
        },
        eq("AA123B"),
      )
    }

    @Test
    fun `test calls Prison API for correct page with sorting of field`() {
      val content = listOf(NomisCaseNote())
      val pageable = PageRequest.of(5, 20, Direction.ASC, "creationDateTime")
      whenever(responseSpecMock.toEntity(any<ParameterizedTypeReference<*>>())).thenReturn(
        Mono.just(ResponseEntity(RestResponsePage(content, pageable, 12), HttpStatus.OK)),
      )
      val response = externalApiService.getOffenderCaseNotes("AA123B", CaseNoteFilter(), pageable)
      assertThat(response.totalElements).isEqualTo(101)
      assertThat(response.content).isEqualTo(content)

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri(
        check {
          val components = UriComponentsBuilder.fromUriString(it).build()
          assertThat(components.path).isEqualTo("/api/offenders/{offenderIdentifier}/case-notes/v2")
          assertThat(components.queryParams).containsExactlyInAnyOrderEntriesOf(
            mapOf("size" to listOf("20"), "page" to listOf("5"), "sort" to listOf("createDatetime,ASC")),
          )
        },
        eq("AA123B"),
      )
    }

    @Test
    fun `test calls Prison API for correct page with sorting of multiple fields`() {
      val content = listOf(NomisCaseNote())
      val pageable = PageRequest.of(5, 20, Direction.ASC, "creationDateTime", "occurrenceDateTime")
      whenever(responseSpecMock.toEntity(any<ParameterizedTypeReference<*>>())).thenReturn(
        Mono.just(ResponseEntity(RestResponsePage(content, pageable, 12), HttpStatus.OK)),
      )
      val response = externalApiService.getOffenderCaseNotes("AA123B", CaseNoteFilter(), pageable)
      assertThat(response.totalElements).isEqualTo(101)
      assertThat(response.content).isEqualTo(content)

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri(
        check {
          val components = UriComponentsBuilder.fromUriString(it).build()
          assertThat(components.path).isEqualTo("/api/offenders/{offenderIdentifier}/case-notes/v2")
          assertThat(components.queryParams)
            .containsEntry("size", listOf("20"))
            .containsEntry("page", listOf("5"))
            .containsKey("sort")
            .hasSize(3)
          assertThat(components.queryParams.get("sort"))
            .containsExactly("createDatetime,ASC", "occurrenceDateTime,ASC")
        },
        eq("AA123B"),
      )
    }

    @Test
    fun `test calls Prison API for correct page with sorting of multiple fields and different directions`() {
      val content = listOf(NomisCaseNote())
      val pageable = PageRequest.of(
        5,
        20,
        Sort.by(Order(Direction.ASC, "creationDateTime"), Order(Direction.DESC, "occurrenceDateTime")),
      )
      whenever(responseSpecMock.toEntity(any<ParameterizedTypeReference<*>>())).thenReturn(
        Mono.just(ResponseEntity(RestResponsePage(content, pageable, 12), HttpStatus.OK)),
      )
      val response = externalApiService.getOffenderCaseNotes("AA123B", CaseNoteFilter(), pageable)
      assertThat(response.totalElements).isEqualTo(101)
      assertThat(response.content).isEqualTo(content)

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri(
        check {
          val components = UriComponentsBuilder.fromUriString(it).build()
          assertThat(components.path).isEqualTo("/api/offenders/{offenderIdentifier}/case-notes/v2")
          assertThat(components.queryParams)
            .containsEntry("size", listOf("20"))
            .containsEntry("page", listOf("5"))
            .containsKey("sort")
            .hasSize(3)
          assertThat(components.queryParams.get("sort"))
            .containsExactly("createDatetime,ASC", "occurrenceDateTime,DESC")
        },
        eq("AA123B"),
      )
    }

    @Test
    fun `test calls Prison API with type filter`() {
      val content = listOf(NomisCaseNote())
      val pageable = Pageable.ofSize(10)
      whenever(responseSpecMock.toEntity(any<ParameterizedTypeReference<*>>())).thenReturn(
        Mono.just(ResponseEntity(RestResponsePage(content, pageable, 12), HttpStatus.OK)),
      )
      val response = externalApiService.getOffenderCaseNotes(
        "AA123B",
        CaseNoteFilter(type = "GEN"),
        pageable,
      )
      assertThat(response.totalElements).isEqualTo(12)
      assertThat(response.content).isEqualTo(content)

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri(
        check {
          val components = UriComponentsBuilder.fromUriString(it).build()
          assertThat(components.path).isEqualTo("/api/offenders/{offenderIdentifier}/case-notes/v2")
          assertThat(components.queryParams).containsExactlyInAnyOrderEntriesOf(
            mapOf("size" to listOf("10"), "page" to listOf("0"), "typeSubTypes" to listOf("{typeSubTypes}")),
          )
        },
        eq("AA123B"),
        eq("GEN"),
      )
    }

    @Test
    fun `test calls Prison API with typeSubTypes filter`() {
      val content = listOf(NomisCaseNote())
      val pageable = Pageable.ofSize(10)
      whenever(responseSpecMock.toEntity(any<ParameterizedTypeReference<*>>())).thenReturn(
        Mono.just(ResponseEntity(RestResponsePage(content, pageable, 12), HttpStatus.OK)),
      )
      val response = externalApiService.getOffenderCaseNotes(
        "AA123B",
        CaseNoteFilter(typeSubTypes = listOf("GEN", "AddType+AddSubType")),
        pageable,
      )
      assertThat(response.totalElements).isEqualTo(12)
      assertThat(response.content).isEqualTo(content)

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri(
        check {
          val components = UriComponentsBuilder.fromUriString(it).build()
          assertThat(components.path).isEqualTo("/api/offenders/{offenderIdentifier}/case-notes/v2")
          assertThat(components.queryParams).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "size" to listOf("10"),
              "page" to listOf("0"),
              "typeSubTypes" to listOf("{typeSubTypes}", "{typeSubTypes}"),
            ),
          )
        },
        eq("AA123B"),
        eq("GEN"),
        eq("AddType+AddSubType"),
      )
    }

    @Test
    fun `test calls Prison API with prisonId filter`() {
      val content = listOf(NomisCaseNote())
      val pageable = Pageable.ofSize(10)
      whenever(responseSpecMock.toEntity(any<ParameterizedTypeReference<*>>())).thenReturn(
        Mono.just(ResponseEntity(RestResponsePage(content, pageable, 12), HttpStatus.OK)),
      )
      val response = externalApiService.getOffenderCaseNotes(
        "AA123B",
        CaseNoteFilter(locationId = "MDI"),
        pageable,
      )
      assertThat(response.totalElements).isEqualTo(12)
      assertThat(response.content).isEqualTo(content)

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri(
        check {
          val components = UriComponentsBuilder.fromUriString(it).build()
          assertThat(components.path).isEqualTo("/api/offenders/{offenderIdentifier}/case-notes/v2")
          assertThat(components.queryParams).containsExactlyInAnyOrderEntriesOf(
            mapOf("size" to listOf("10"), "page" to listOf("0"), "prisonId" to listOf("MDI")),
          )
        },
        eq("AA123B"),
      )
    }

    @Test
    fun `test calls Prison API with from filter`() {
      val content = listOf(NomisCaseNote())
      val pageable = Pageable.ofSize(10)
      whenever(responseSpecMock.toEntity(any<ParameterizedTypeReference<*>>())).thenReturn(
        Mono.just(ResponseEntity(RestResponsePage(content, pageable, 12), HttpStatus.OK)),
      )
      val response = externalApiService.getOffenderCaseNotes(
        "AA123B",
        CaseNoteFilter(startDate = LocalDateTime.parse("2023-01-02T10:20:30")),
        pageable,
      )
      assertThat(response.totalElements).isEqualTo(12)
      assertThat(response.content).isEqualTo(content)

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri(
        check {
          val components = UriComponentsBuilder.fromUriString(it).build()
          assertThat(components.path).isEqualTo("/api/offenders/{offenderIdentifier}/case-notes/v2")
          assertThat(components.queryParams).containsExactlyInAnyOrderEntriesOf(
            mapOf("size" to listOf("10"), "page" to listOf("0"), "from" to listOf("2023-01-02")),
          )
        },
        eq("AA123B"),
      )
    }

    @Test
    fun `test calls Prison API with to filter`() {
      val content = listOf(NomisCaseNote())
      val pageable = Pageable.ofSize(10)
      whenever(responseSpecMock.toEntity(any<ParameterizedTypeReference<*>>())).thenReturn(
        Mono.just(ResponseEntity(RestResponsePage(content, pageable, 12), HttpStatus.OK)),
      )
      val response = externalApiService.getOffenderCaseNotes(
        "AA123B",
        CaseNoteFilter(endDate = LocalDateTime.parse("2023-02-03T10:20:30")),
        pageable,
      )
      assertThat(response.totalElements).isEqualTo(12)
      assertThat(response.content).isEqualTo(content)

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri(
        check {
          val components = UriComponentsBuilder.fromUriString(it).build()
          assertThat(components.path).isEqualTo("/api/offenders/{offenderIdentifier}/case-notes/v2")
          assertThat(components.queryParams).containsExactlyInAnyOrderEntriesOf(
            mapOf("size" to listOf("10"), "page" to listOf("0"), "to" to listOf("2023-02-03")),
          )
        },
        eq("AA123B"),
      )
    }
  }

  @Nested
  inner class createCaseNote {
    @Test
    fun `test calls Prison API`() {
      val result = NomisCaseNote()

      whenever(responseSpecMock.bodyToMono(any<ParameterizedTypeReference<NomisCaseNote>>())).thenReturn(
        Mono.just(result),
      )
      val postData = CreateCaseNoteRequest("MDI", "ACP", "POS1", LocalDateTime.now(), "Some text", false)
      whenever(prisonApiWebClient.post()).thenReturn(requestBodyUriSpec)
      whenever(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec)

      assertThat(externalApiService.createCaseNote("AA123B", postData)).isSameAs(result)

      verify(prisonApiWebClient).post()
      verify(requestBodySpec).bodyValue(postData)
      verify(requestBodyUriSpec).uri("/api/offenders/{offenderNo}/case-notes", "AA123B")
    }
  }

  @Nested
  inner class getOffenderCaseNote {
    @Test
    fun `test calls Prison API`() {
      val result = NomisCaseNote()
      whenever(responseSpecMock.bodyToMono(any<ParameterizedTypeReference<NomisCaseNote>>())).thenReturn(
        Mono.just(result),
      )
      assertThat(externalApiService.getOffenderCaseNote("AA123B", 12345)).isSameAs(result)

      verify(prisonApiWebClient).get()
      verify(requestHeadersUriSpec).uri("/api/offenders/{offenderNo}/case-notes/{caseNoteIdentifier}", "AA123B", 12345L)
    }
  }

  @Nested
  inner class amendOffenderCaseNote {
    @Test
    fun `test calls Prison API`() {
      val result = NomisCaseNote()
      whenever(responseSpecMock.bodyToMono(any<ParameterizedTypeReference<NomisCaseNote>>())).thenReturn(
        Mono.just(result),
      )
      val postData = UpdateCaseNote()
      whenever(prisonApiWebClient.put()).thenReturn(requestBodyUriSpec)
      whenever(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec)
      assertThat(externalApiService.amendOffenderCaseNote("AA123B", 12345, postData)).isSameAs(result)

      verify(prisonApiWebClient).put()
      verify(requestBodySpec).bodyValue(postData)
      verify(requestBodyUriSpec).uri("/api/offenders/{offenderNo}/case-notes/{caseNoteIdentifier}", "AA123B", 12345L)
    }
  }
}
