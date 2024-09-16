package uk.gov.justice.hmpps.casenotes.controllers

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.config.ServiceConfig
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.notes.AmendCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.notes.CreateCaseNoteRequest
import uk.gov.justice.hmpps.casenotes.notes.ReadCaseNote
import uk.gov.justice.hmpps.casenotes.notes.WriteCaseNote
import uk.gov.justice.hmpps.casenotes.services.CaseNoteEventPusher
import uk.gov.justice.hmpps.casenotes.services.CaseNoteService
import uk.gov.justice.hmpps.casenotes.services.ExternalApiService

const val CASELOAD_ID = "CaseloadId"

@Tag(name = "case-notes", description = "Case Note Controller")
@RestController
@RequestMapping("case-notes")
class CaseNoteController(
  private val serviceConfig: ServiceConfig,
  private val caseNoteService: CaseNoteService,
  private val find: ReadCaseNote,
  private val save: WriteCaseNote,
  private val telemetryClient: TelemetryClient,
  private val securityUserContext: SecurityUserContext,
  private val caseNoteEventPusher: CaseNoteEventPusher,
  private val externalApiService: ExternalApiService,
) {
  @Operation(summary = "Retrieves a case note")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "404",
      description = "Offender or case note not found",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  @GetMapping("/{personIdentifier}/{caseNoteIdentifier}")
  fun getCaseNote(
    @Parameter(description = "Person Identifier", required = true, example = "A1234AA")
    @PathVariable personIdentifier: String,
    @Parameter(description = "Case Note Id", required = true, example = "518b2200-6489-4c77-8514-10cf80ccd488")
    @PathVariable caseNoteIdentifier: String,
    @RequestHeader(CASELOAD_ID) caseloadId: String? = null,
  ): CaseNote {
    return if (caseloadId in serviceConfig.activePrisons) {
      find.caseNote(personIdentifier, caseNoteIdentifier)
    } else {
      caseNoteService.getCaseNote(personIdentifier, caseNoteIdentifier)
    }
  }

  @Operation(
    summary = "Retrieves a list of case notes",
    description = "Sorting can be applied on occurrenceDateTime (default) or creationDateTime. Any other sort parameter will have the default result (occurrenceDateTime,desc)",
  )
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "404",
      description = "Offender not found",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  @GetMapping("/{personIdentifier}")
  fun getCaseNotes(
    @Parameter(description = "Person Identifier", required = true, example = "A1234AA")
    @PathVariable personIdentifier: String,
    @Parameter(description = "Optionally specify a case note filter") filter: CaseNoteFilter,
    @PageableDefault(sort = ["occurrenceDateTime"], direction = Sort.Direction.DESC) pageable: Pageable,
    @RequestHeader(CASELOAD_ID) caseloadId: String? = null,
  ): Page<CaseNote> {
    return if (caseloadId in serviceConfig.activePrisons) {
      find.caseNotes(personIdentifier, filter, pageable)
    } else {
      caseNoteService.getCaseNotes(personIdentifier, filter, pageable)
    }
  }

  @Operation(
    summary = "Add a user supplied case note for an offender.",
    description =
    """
    This endpoint should be used for adding a user supplied case note for an offender.
    
    System generated case notes, i.e. those generated as a side effect of another service, are supported but their usage is discouraged. 
    HMPPS intends case notes to only be supplied by users. Case notes used to inform of a change in another service, for example alerts being added or closed, should be considered deprecated and alternative methods of displaying this information to users should be pursued.
    
    An example alternative solution: a dedicated widget showing summary information on the prisoner profile provides improved context at a glance about a prisoner.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The Case Note has been recorded. The updated object is returned including the status.",
      ),
      ApiResponse(
        responseCode = "409",
        description = "The case note has already been recorded under the booking. The current unmodified object (including status) is returned.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{personIdentifier}")
  fun createCaseNote(
    @Parameter(description = "Person Identifier", required = true, example = "A1234AA")
    @PathVariable personIdentifier: String,
    @Valid @RequestBody createCaseNote: CreateCaseNoteRequest,
    @Parameter(description = "Boolean to indicate that the user creating the case note has privileges to use restricted use case note types")
    @RequestParam(required = false, defaultValue = "false") useRestrictedType: Boolean,
    @RequestHeader(required = false, value = CASELOAD_ID) caseloadId: String? = null,
  ): CaseNote {
    val caseNote = if (caseloadId in serviceConfig.activePrisons) {
      val request = if (createCaseNote.locationId == null) {
        createCaseNote.copy(locationId = externalApiService.getOffenderLocation(personIdentifier))
      } else {
        createCaseNote
      }
      save.createNote(personIdentifier, request, useRestrictedType)
    } else {
      caseNoteService.createCaseNote(personIdentifier, createCaseNote)
    }

    telemetryClient.trackEvent("CaseNoteCreated", createEventProperties(caseNote), null)
    caseNoteEventPusher.sendEvent(caseNote)
    return caseNote
  }

  @Operation(
    summary = "Amend Case Note for offender",
    description = "Amend a case note information adds and additional entry to the note",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The Case Note has been recorded. The updated object is returned including the status.",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No case notes where found for this offender and case note id",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/{personIdentifier}/{caseNoteIdentifier}")
  fun amendCaseNote(
    @Parameter(description = "Person Identifier", required = true, example = "A1234AA")
    @PathVariable personIdentifier: String,
    @Parameter(description = "Case Note Id", required = true, example = "518b2200-6489-4c77-8514-10cf80ccd488")
    @PathVariable caseNoteIdentifier: String,
    @Valid @RequestBody amendedText: AmendCaseNoteRequest,
    @RequestParam(required = false, defaultValue = "false") useRestrictedType: Boolean,
    @RequestHeader(required = false, value = CASELOAD_ID) caseloadId: String? = null,
  ): CaseNote {
    val caseNote = if (caseloadId in serviceConfig.activePrisons) {
      save.createAmendment(personIdentifier, caseNoteIdentifier, amendedText, useRestrictedType)
    } else {
      caseNoteService.amendCaseNote(personIdentifier, caseNoteIdentifier, amendedText)
    }
    telemetryClient.trackEvent("CaseNoteUpdated", createEventProperties(caseNote), null)
    caseNoteEventPusher.sendEvent(caseNote)
    return caseNote
  }

  @Operation(summary = "Deletes a case note")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "404",
      description = "Offender or case note not found",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  @ResponseStatus(HttpStatus.OK)
  @DeleteMapping("/{personIdentifier}/{caseNoteId}")
  fun softDeleteCaseNote(
    @Parameter(description = "Person Identifier", required = true, example = "A1234AA")
    @PathVariable personIdentifier: String,
    @Parameter(description = "Case Note Id", required = true, example = "518b2200-6489-4c77-8514-10cf80ccd488")
    @PathVariable caseNoteId: String,
    @RequestHeader(CASELOAD_ID) caseloadId: String? = null,
  ) {
    if (caseloadId in serviceConfig.activePrisons) {
      save.deleteNote(personIdentifier, caseNoteId)
    } else {
      caseNoteService.deleteCaseNote(personIdentifier, caseNoteId)
    }

    telemetryClient.trackEvent(
      "CaseNoteDelete",
      mapOf(
        "userName" to CaseNoteRequestContext.get().username,
        "personIdentifier" to personIdentifier,
        "caseNoteId" to caseNoteId,
      ),
      null,
    )
  }

  private fun createEventProperties(caseNote: CaseNote): Map<String, String> {
    return java.util.Map.of(
      "caseNoteId", caseNote.caseNoteId,
      "caseNoteType", String.format("%s-%s", caseNote.type, caseNote.subType),
      "type", caseNote.type,
      "subType", caseNote.subType,
      "personIdentifier", caseNote.personIdentifier,
      "authorUsername", securityUserContext.getCurrentUsername() ?: "unknown",
    )
  }
}
