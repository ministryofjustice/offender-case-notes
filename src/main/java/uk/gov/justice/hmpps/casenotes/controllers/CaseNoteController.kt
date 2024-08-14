package uk.gov.justice.hmpps.casenotes.controllers

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteTypeDto
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNote
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNoteType
import uk.gov.justice.hmpps.casenotes.services.CaseNoteEventPusher
import uk.gov.justice.hmpps.casenotes.services.CaseNoteService
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType
import uk.gov.justice.hmpps.casenotes.types.CaseNoteTypeService
import uk.gov.justice.hmpps.casenotes.types.CreateParentType
import uk.gov.justice.hmpps.casenotes.types.CreateSubType

@Tag(name = "case-notes", description = "Case Note Controller")
@RestController
@RequestMapping("case-notes")
class CaseNoteController(
  private val caseNoteService: CaseNoteService,
  private val telemetryClient: TelemetryClient,
  private val securityUserContext: SecurityUserContext,
  private val caseNoteEventPusher: CaseNoteEventPusher,
  private val caseNoteTypeService: CaseNoteTypeService,
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
  @GetMapping("/{offenderIdentifier}/{caseNoteIdentifier}")
  fun getCaseNote(
    @Parameter(description = "Offender Identifier", required = true, example = "A1234AA")
    @PathVariable offenderIdentifier: String,
    @Parameter(description = "Case Note Id", required = true, example = "518b2200-6489-4c77-8514-10cf80ccd488")
    @PathVariable caseNoteIdentifier: String,
  ): CaseNote = caseNoteService.getCaseNote(offenderIdentifier, caseNoteIdentifier)

  @Operation(summary = "Retrieves a list of case notes")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "404",
      description = "Offender not found",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  @GetMapping("/{offenderIdentifier}")
  fun getCaseNotes(
    @Parameter(description = "Offender Identifier", required = true, example = "A1234AA")
    @PathVariable offenderIdentifier: String,
    @Parameter(description = "Optionally specify a case note filter") filter: CaseNoteFilter?,
    @PageableDefault(sort = ["occurrenceDateTime"], direction = Sort.Direction.DESC) pageable: Pageable?,
  ): Page<CaseNote> = caseNoteService.getCaseNotes(offenderIdentifier, filter, pageable)

  @Operation(summary = "Add Case Note for offender", description = "Creates a note for a specific type/subType")
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
  @PostMapping("/{offenderIdentifier}")
  fun createCaseNote(
    @Parameter(description = "Offender Identifier", required = true, example = "A1234AA")
    @PathVariable offenderIdentifier: String,
    @RequestBody newCaseNote: NewCaseNote,
  ): CaseNote = caseNoteService.createCaseNote(offenderIdentifier, newCaseNote).also {
    telemetryClient.trackEvent("CaseNoteCreated", createEventProperties(it), null)
    caseNoteEventPusher.sendEvent(it)
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
  @PutMapping("/{offenderIdentifier}/{caseNoteIdentifier}")
  fun amendCaseNote(
    @Parameter(description = "Offender Identifier", required = true, example = "A1234AA")
    @PathVariable offenderIdentifier: String,
    @Parameter(description = "Case Note Id", required = true, example = "518b2200-6489-4c77-8514-10cf80ccd488")
    @PathVariable caseNoteIdentifier: String,
    @RequestBody amendedText: UpdateCaseNote,
  ): CaseNote = caseNoteService.amendCaseNote(offenderIdentifier, caseNoteIdentifier, amendedText).also {
    telemetryClient.trackEvent("CaseNoteUpdated", createEventProperties(it), null)
    caseNoteEventPusher.sendEvent(it)
  }

  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
    ),
    ApiResponse(
      responseCode = "404",
      description = "Case notes types not found",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ErrorResponse::class),
        ),
      ],
    ),
  )
  @Operation(summary = "Retrieves a list of case note types")
  @GetMapping("/types")
  fun getCaseNoteTypes(): List<CaseNoteTypeDto> = caseNoteService.caseNoteTypes

  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
    ),
    ApiResponse(
      responseCode = "404",
      description = "Case notes types not found",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ErrorResponse::class),
        ),
      ],
    ),
  )
  @Operation(summary = "Retrieves a list of case note types for this user")
  @GetMapping("/types-for-user")
  fun getUserCaseNoteTypes(): List<CaseNoteTypeDto> = caseNoteService.userCaseNoteTypes

  @Operation(summary = "Add New Case Note Type", description = "Creates a new case note type")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The Case Note Type has been recorded. The updated object is returned including the status.",
      ),
      ApiResponse(
        responseCode = "409",
        description = "The case note type has already been recorded. The current unmodified object (including status) is returned.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/types")
  fun createCaseNoteType(@RequestBody body: CreateParentType): CaseNoteType = caseNoteTypeService.createParentType(body)

  @Operation(summary = "Add New Case Note Sub Type", description = "Creates a new case note sub type")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The Case Note Sub Type has been recorded. The updated object is returned including the status.",
      ),
      ApiResponse(
        responseCode = "409",
        description = "The case note sub type has already been recorded. The current unmodified object (including status) is returned.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/types/{parentType}")
  fun createCaseNoteSubType(
    @Parameter(description = "Parent Case Note Type", required = true, example = "GEN")
    @PathVariable parentType: String,
    @RequestBody body: CreateSubType,
  ): CaseNoteType = caseNoteTypeService.createSubType(parentType, body)

  @Operation(summary = "Update Case Note Type", description = "Creates a new case note type")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The case note type has been updated. The updated object is returned.",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The case note type is not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  @PutMapping("/types/{parentType}")
  fun updateCaseNoteType(
    @Parameter(description = "Parent Case Note Type", required = true, example = "OBS")
    @PathVariable parentType: String,
    @RequestBody body: UpdateCaseNoteType,
  ): CaseNoteTypeDto = caseNoteService.updateCaseNoteType(parentType, body)

  @Operation(summary = "Update Case Note Sub Type", description = "Creates a new case note sub type")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The case note sub type update has been updated. The updated object is returned.",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The case note sub type is not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  @PutMapping("/types/{parentType}/{subType}")
  fun updateCaseNoteSubType(
    @Parameter(description = "Parent Case Note Type", required = true, example = "OBS")
    @PathVariable parentType: String,
    @Parameter(description = "Sub Case Note Type", required = true, example = "GEN")
    @PathVariable subType: String,
    @RequestBody body: UpdateCaseNoteType,
  ): CaseNoteTypeDto = caseNoteService.updateCaseNoteSubType(parentType, subType, body)

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
  @DeleteMapping("/{offenderIdentifier}/{caseNoteId}")
  fun softDeleteCaseNote(
    @Parameter(description = "Offender Identifier", required = true, example = "A1234AA")
    @PathVariable offenderIdentifier: String,
    @Parameter(description = "Case Note Id", required = true, example = "518b2200-6489-4c77-8514-10cf80ccd488")
    @PathVariable caseNoteId: String,
  ) = caseNoteService.softDeleteCaseNote(offenderIdentifier, caseNoteId)

  @Operation(summary = "Deletes a case note amendment")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "404",
      description = "Offender or case note not found",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  @ResponseStatus(HttpStatus.OK)
  @DeleteMapping("/amendment/{offenderIdentifier}/{caseNoteAmendmentId}")
  fun softDeleteCaseNoteAmendment(
    @Parameter(description = "Offender Identifier", required = true, example = "A1234AA")
    @PathVariable offenderIdentifier: String,
    @Parameter(description = "Case Note Amendment Id", required = true, example = "1")
    @PathVariable caseNoteAmendmentId: Long?,
  ) = caseNoteService.softDeleteCaseNoteAmendment(offenderIdentifier, caseNoteAmendmentId)

  private fun createEventProperties(caseNote: CaseNote): Map<String, String> {
    return java.util.Map.of(
      "caseNoteId", caseNote.caseNoteId,
      "caseNoteType", String.format("%s-%s", caseNote.type, caseNote.subType),
      "type", caseNote.type,
      "subType", caseNote.subType,
      "offenderIdentifier", caseNote.offenderIdentifier,
      "authorUsername", securityUserContext.getCurrentUsername() ?: "unknown",
    )
  }
}
