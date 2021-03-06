package uk.gov.justice.hmpps.casenotes.controllers

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import uk.gov.justice.hmpps.casenotes.services.PublishNoteService
import java.time.LocalDateTime
import javax.validation.constraints.NotNull

@Api(tags = ["publish-notes"])
@RestController
@RequestMapping(value = ["publish-notes"], produces = [APPLICATION_JSON_VALUE])
class PublishNotesController(private val publishNoteService: PublishNoteService) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping
  @ApiOperation(value = "Publish sensitive case notes", nickname = "publish sensitive case notes")
  @ApiResponses(ApiResponse(code = 200, message = "Number of notes to be published (asynchronously)"))
  fun publishCaseNotes(
    @ApiParam(value = "A timestamp that indicates the earliest record required")
    @RequestParam("fromDateTime", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) fromDateTime: LocalDateTime?,
    @ApiParam(value = "A timestamp that indicates the latest record required", required = true)
    @NotNull @RequestParam("toDateTime") @DateTimeFormat(iso = ISO.DATE_TIME) toDateTime: LocalDateTime
  ): Int {

    val caseNotes: List<CaseNote> = publishNoteService.findCaseNotes(
      fromDateTime
        ?: LocalDateTime.parse("2019-01-01T00:00:00"),
      toDateTime
    )
    log.info("Found {} notes to publish", caseNotes.size)
    publishNoteService.pushCaseNotes(caseNotes)

    return caseNotes.size
  }
}
