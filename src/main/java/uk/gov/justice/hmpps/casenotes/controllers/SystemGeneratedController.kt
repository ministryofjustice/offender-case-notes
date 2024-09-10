package uk.gov.justice.hmpps.casenotes.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder.getRequestAttributes
import uk.gov.justice.hmpps.casenotes.config.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.casenotes.config.CaseNoteRequestContext
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext.Companion.ROLE_SYSTEM_GENERATED_RW
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse
import uk.gov.justice.hmpps.casenotes.notes.CaseNote
import uk.gov.justice.hmpps.casenotes.services.CaseNoteEventPusher
import uk.gov.justice.hmpps.casenotes.systemgenerated.CreateSysGenNote
import uk.gov.justice.hmpps.casenotes.systemgenerated.SystemGeneratedRequest

@Tag(name = "System Generated Case Notes", description = "Endpoints system generated case notes")
@RestController
@RequestMapping("system-generated/case-notes/{personIdentifier}")
class SystemGeneratedController(
  private val save: CreateSysGenNote,
  private val eventPusher: CaseNoteEventPusher,
) {

  @Operation(summary = "Endpoint to create system generated case notes.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Case note successfully created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('$ROLE_SYSTEM_GENERATED_RW')")
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  fun createSystemGeneratedNote(
    @PathVariable personIdentifier: String,
    @RequestBody request: SystemGeneratedRequest,
  ): CaseNote {
    setContext()
    return save.systemGeneratedCaseNote(personIdentifier, request).also { eventPusher.sendEvent(it) }
  }

  private fun setContext() {
    val name = (SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken).name
    checkNotNull(getRequestAttributes()).setAttribute(
      CaseNoteRequestContext::class.simpleName!!,
      CaseNoteRequestContext(name),
      0,
    )
  }
}
