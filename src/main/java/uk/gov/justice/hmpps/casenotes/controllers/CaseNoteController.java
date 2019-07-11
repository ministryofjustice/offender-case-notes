package uk.gov.justice.hmpps.casenotes.controllers;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.gov.justice.hmpps.casenotes.dto.CaseNote;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote;
import uk.gov.justice.hmpps.casenotes.services.CaseNoteService;

import javax.validation.constraints.NotNull;
import java.util.List;

@Api(tags = {"case-notes"})
@RestController
@RequestMapping(
        value="case-notes",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class CaseNoteController {

    private final CaseNoteService caseNoteService;

    public CaseNoteController(CaseNoteService caseNoteService) {
        this.caseNoteService = caseNoteService;
    }

    @GetMapping("/{offenderIdentifier}")
    @ApiOperation(  value = "Returns list of case note for this offender",
                    response = CaseNote.class,
                    notes = "More Information Here")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Case Note list.", response = List.class),
            @ApiResponse(code = 404, message = "No case notes where found for this offender", response = ErrorResponse.class)})
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", dataType = "int", paramType = "query",
                    value = "Results page you want to retrieve (0..N)", example = "0", defaultValue = "0"),
            @ApiImplicitParam(name = "size", dataType = "int", paramType = "query",
                    value = "Number of records per page.", example = "10", defaultValue = "10"),
            @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
                    value = "Sort column and direction, eg sort=occurrenceDateTime,desc")})
    public Page<CaseNote> getCaseNotesByOffenderIdentifier(
            @ApiParam(value = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
            @PageableDefault(sort = {"occurrenceDateTime"}, direction = Sort.Direction.DESC) final Pageable pageable) {
        return caseNoteService.getCaseNotesByOffenderIdentifier(offenderIdentifier, pageable);
    }

    @PostMapping(value = "/{offenderIdentifier}", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(  value = "Add Case Note for offender",
            response = CaseNote.class,
            notes = "Creates a note for a specific type/subType")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "The Case Note has been recorded. The updated object is returned including the status.", response = CaseNote.class),
            @ApiResponse(code = 409, message = "The case note has already been recorded under the booking. The current unmodified object (including status) is returned.", response = ErrorResponse.class)})

    public CaseNote createCaseNote(
            @ApiParam(value = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
            @RequestBody @NotNull final NewCaseNote newCaseNote) {
        return caseNoteService.createCaseNote(offenderIdentifier, newCaseNote);
    }

    @PutMapping(value = "/{offenderIdentifier}/{caseNoteIdentifier}", consumes = "application/json")
    @ApiOperation(  value = "Amend Case Note for offender",
            response = CaseNote.class,
            notes = "Amend a case note information adds and additional entry to the note")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "The Case Note has been recorded. The updated object is returned including the status.", response = CaseNote.class),
            @ApiResponse(code = 404, message = "No case notes where found for this offender and case note id", response = ErrorResponse.class)})

    public CaseNote amendCaseNote(
            @ApiParam(value = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
            @ApiParam(value = "Case Note Id", required = true, example = "A1234AA") @PathVariable("caseNoteIdentifier") final Long caseNoteIdentifier,
            @RequestBody @NotNull final String amendedText) {
        return caseNoteService.amendCaseNote(offenderIdentifier, caseNoteIdentifier, amendedText);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "Retrieves a list of case notes",
            nickname = "retrieve case notes")
    @ApiResponses({
            @ApiResponse(code = 404, message = "Case notes not found"),
            @ApiResponse(code = 200, message = "OK")})
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", dataType = "int", paramType = "query",
                    value = "Results page you want to retrieve (0..N)", example = "0", defaultValue = "0"),
            @ApiImplicitParam(name = "size", dataType = "int", paramType = "query",
                    value = "Number of records per page.", example = "10", defaultValue = "10"),
            @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
                    value = "Sort column and direction, eg sort=occurrenceDateTime,asc. Multiple sort params allowed.")})
    public Page<CaseNote> getCaseNotes(@ApiParam(value = "Optionally specify a case note filter")
                                        CaseNoteFilter filter,
                                       @PageableDefault(sort = {"occurrenceDateTime", "locationId"}, direction = Sort.Direction.ASC) final Pageable pageable) {

       return caseNoteService.getCaseNotes(filter, pageable);
    }
}
