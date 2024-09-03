package uk.gov.justice.hmpps.casenotes.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.types.CaseNoteType
import uk.gov.justice.hmpps.casenotes.types.ReadCaseNoteType
import uk.gov.justice.hmpps.casenotes.types.SelectableBy

@Tag(name = "case-note-types", description = "Case Note Types Controller")
@RestController
@RequestMapping("case-notes")
class CaseNoteTypeController(private val readCaseNoteType: ReadCaseNoteType) {
  /* Temporarily disabled until issue with role check resolved @PreAuthorize("hasAnyRole('$ROLE_CASE_NOTES_READ', '$ROLE_CASE_NOTES_WRITE')") */
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
  Additionally, the following properties allow filtering: 
  includeInactive -> if this is true the returned results will include inactive types, otherwise only active types will be returned.
  includeRestricted -> if this is true the returned results will include restricted use types, otherwise only non-restricted types will be returned.
  """,
  )
  @GetMapping("/types")
  fun getCaseNoteTypes(
    @RequestParam(required = false) selectableBy: SelectableBy = SelectableBy.ALL,
    @RequestParam(required = false) includeInactive: Boolean = true,
    @RequestParam(required = false) includeRestricted: Boolean = true,
  ): List<CaseNoteType> = readCaseNoteType.getCaseNoteTypes(selectableBy, includeInactive, includeRestricted)

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
