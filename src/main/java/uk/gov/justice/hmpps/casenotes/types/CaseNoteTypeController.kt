package uk.gov.justice.hmpps.casenotes.types

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.types.TypeInclude.INACTIVE
import uk.gov.justice.hmpps.casenotes.types.internal.ReadCaseNoteType
import uk.gov.justice.hmpps.casenotes.types.internal.WriteCaseNoteType

@Tag(name = "case-note-types", description = "Case Note Types Controller")
@RestController
@RequestMapping("case-notes")
class CaseNoteTypeController(
  private val readCaseNoteType: ReadCaseNoteType,
  private val writeCaseNoteType: WriteCaseNoteType,
) {
  @Operation(summary = "Add New Case Note Type")
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
  fun createCaseNoteType(@RequestBody body: CreateParentType): CaseNoteType = writeCaseNoteType.createParentType(body)

  @Operation(summary = "Add New Case Note Sub Type")
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
  ): CaseNoteType = writeCaseNoteType.createSubType(parentType, body)

  @Operation(summary = "Update Case Note Type")
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
    @RequestBody body: UpdateParentType,
  ): CaseNoteType = writeCaseNoteType.updateParentType(parentType, body)

  @Operation(summary = "Update Case Note Sub Type")
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
    @RequestBody body: UpdateSubType,
  ): CaseNoteType = writeCaseNoteType.updateSubType(parentType, subType, body)

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
  @Operation(
    summary = "Retrieves a list of case note types",
    description =
    """
  Please note, current functionality of using user roles from the token is now deprecated. 
  Going forward user roles will not affect the results returned from this endpoint.
  This has been replaced with optional request params to replicate the same functionality.
  Setting a value for 'selectableBy' will allow the choice of filtering those that are selectable by dps users only or to include all types regardless.
  Additionally, a list of values are available for the include filter: 
  INACTIVE -> if this is present the returned results will include inactive types, otherwise only active types will be returned.
  SENSITIVE -> if this is present the returned results will include sensitive types, otherwise only non-sensitive types will be returned.
  RESTRICTED -> if this is present the returned results will include restricted types, otherwise only non-restricted types will be returned.
  If an include param is not provided, the defaults will provide the same result as though you had provided INACTIVE ie providing types that are both active and inactive.
  """,
  )
  @GetMapping("/types")
  fun getCaseNoteTypes(
    @RequestParam selectableBy: SelectableBy = SelectableBy.ALL,
    @RequestParam include: Set<TypeInclude> = setOf(INACTIVE),
  ): List<CaseNoteType> = readCaseNoteType.getCaseNoteTypes(selectableBy, include)

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
  @Operation(
    summary = "(Deprecated) Retrieves a list of case note types for this user",
    deprecated = true,
    description =
    """
    This endpoint is due to be removed. The same functionality can be achieved using '/types'.
    To replicate the behaviour of this endpoint, please pass in a request params of 'selectableBy=DPS_USER' and 'include='
    """,
  )
  @GetMapping("/types-for-user")
  fun getUserCaseNoteTypes(): List<CaseNoteType> = readCaseNoteType.getUserCaseNoteTypes()
}
