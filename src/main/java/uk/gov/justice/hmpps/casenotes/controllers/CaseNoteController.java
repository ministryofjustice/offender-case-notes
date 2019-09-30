package uk.gov.justice.hmpps.casenotes.controllers;

import com.microsoft.applicationinsights.TelemetryClient;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;
import uk.gov.justice.hmpps.casenotes.dto.*;
import uk.gov.justice.hmpps.casenotes.services.CaseNoteService;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.format.annotation.DateTimeFormat.ISO;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Api(tags = {"case-notes"})
@RestController
@RequestMapping(
        value = "case-notes",
        produces = APPLICATION_JSON_VALUE)
@Slf4j
@AllArgsConstructor
public class CaseNoteController {

    private final CaseNoteService caseNoteService;
    private final TelemetryClient telemetryClient;
    private final SecurityUserContext securityUserContext;

    @GetMapping("/{offenderIdentifier}/{caseNoteIdentifier}")
    @ResponseBody
    @ApiOperation(value = "Retrieves a case note",
            nickname = "retrieve case note")
    @ApiResponses({
            @ApiResponse(code = 404, message = "Offender or case note not found"),
            @ApiResponse(code = 200, message = "OK", response = CaseNote.class, responseContainer = "List")})
    public CaseNote getCaseNote(
            @ApiParam(value = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
            @ApiParam(value = "Case Note Id", required = true, example = "A1234AA") @PathVariable("caseNoteIdentifier") final String caseNoteIdentifier) {
        return caseNoteService.getCaseNote(offenderIdentifier, caseNoteIdentifier);
    }

    @GetMapping("/{offenderIdentifier}")
    @ResponseBody
    @ApiOperation(value = "Retrieves a list of case notes",
            nickname = "retrieve case notes")
    @ApiResponses({
            @ApiResponse(code = 404, message = "Offender not found"),
            @ApiResponse(code = 200, message = "OK", response = CaseNote.class, responseContainer = "List")})
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", dataType = "int", paramType = "query",
                    value = "Results page you want to retrieve (0..N)", example = "0", defaultValue = "0"),
            @ApiImplicitParam(name = "size", dataType = "int", paramType = "query",
                    value = "Number of records per page.", example = "10", defaultValue = "10"),
            @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
                    value = "Sort column and direction, e.g. sort=occurrenceDateTime,desc. Multiple sort params allowed.")})
    public Page<CaseNote> getCaseNotes(
            @ApiParam(value = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
            @ApiParam(value = "Optionally specify a case note filter") final CaseNoteFilter filter,
            @PageableDefault(sort = {"occurrenceDateTime"}, direction = Sort.Direction.DESC) final Pageable pageable) {
        return caseNoteService.getCaseNotes(offenderIdentifier, filter, pageable);
    }

    @PostMapping(value = "/{offenderIdentifier}", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "Add Case Note for offender",
            response = CaseNote.class,
            notes = "Creates a note for a specific type/subType")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "The Case Note has been recorded. The updated object is returned including the status.", response = CaseNote.class),
            @ApiResponse(code = 409, message = "The case note has already been recorded under the booking. The current unmodified object (including status) is returned.", response = ErrorResponse.class)})
    public CaseNote createCaseNote(
            @ApiParam(value = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
            @RequestBody @NotNull final NewCaseNote newCaseNote) {
        final var caseNoteCreated = caseNoteService.createCaseNote(offenderIdentifier, newCaseNote);
        // Log event
        telemetryClient.trackEvent("CaseNoteCreated", createEventProperties(caseNoteCreated), null);
        return caseNoteCreated;
    }

    @PutMapping(value = "/{offenderIdentifier}/{caseNoteIdentifier}", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Amend Case Note for offender",
            response = CaseNote.class,
            notes = "Amend a case note information adds and additional entry to the note")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "The Case Note has been recorded. The updated object is returned including the status.", response = CaseNote.class),
            @ApiResponse(code = 404, message = "No case notes where found for this offender and case note id", response = ErrorResponse.class)})
    public CaseNote amendCaseNote(
            @ApiParam(value = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
            @ApiParam(value = "Case Note Id", required = true, example = "A1234AA") @PathVariable("caseNoteIdentifier") final String caseNoteIdentifier,
            @RequestBody @NotNull final UpdateCaseNote amendedText) {
        final var amendCaseNote = caseNoteService.amendCaseNote(offenderIdentifier, caseNoteIdentifier, amendedText);

        // Log event
        telemetryClient.trackEvent("CaseNoteUpdated", createEventProperties(amendCaseNote), null);
        return amendCaseNote;
    }

    @GetMapping(value = "/types")
    @ApiOperation(value = "Retrieves a list of case note types", response = CaseNoteType.class)
    @ApiResponses({
            @ApiResponse(code = 404, message = "Case notes types not found"),
            @ApiResponse(code = 200, message = "OK", response = CaseNoteType.class, responseContainer = "List")})
    public List<CaseNoteType> getCaseNoteTypes() {
        return caseNoteService.getCaseNoteTypes();
    }

    @GetMapping("/types-for-user")
    @ApiOperation(value = "Retrieves a list of case note types for this user", response = CaseNoteType.class)
    @ApiResponses({
            @ApiResponse(code = 404, message = "Case notes types not found"),
            @ApiResponse(code = 200, message = "OK", response = CaseNoteType.class, responseContainer = "List")})
    public List<CaseNoteType> getUserCaseNoteTypes() {
        return caseNoteService.getUserCaseNoteTypes();

    }

    @PostMapping(value = "/types", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "Add New Case Note Type",
            response = NewCaseNoteType.class,
            notes = "Creates a new case note type")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "The Case Note Type has been recorded. The updated object is returned including the status.", response = CaseNoteType.class),
            @ApiResponse(code = 409, message = "The case note type has already been recorded. The current unmodified object (including status) is returned.", response = ErrorResponse.class)})
    public CaseNoteType createCaseNoteType(@RequestBody @NotNull final NewCaseNoteType body) {
        return caseNoteService.createCaseNoteType(body);
    }

    @PostMapping(value = "/types/{parentType}", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "Add New Case Note Sub Type",
            response = NewCaseNoteType.class,
            notes = "Creates a new case note sub type")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "The Case Note Sub Type has been recorded. The updated object is returned including the status.", response = CaseNoteType.class),
            @ApiResponse(code = 409, message = "The case note sub type has already been recorded. The current unmodified object (including status) is returned.", response = ErrorResponse.class)})
    public CaseNoteType createCaseNoteSubType(
            @ApiParam(value = "Parent Case Note Type", required = true, example = "GEN") @PathVariable("parentType") final String parentType,
            @RequestBody @NotNull final NewCaseNoteType body) {
        return caseNoteService.createCaseNoteSubType(parentType, body);
    }

    @PutMapping(value = "/types/{parentType}", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Update Case Note Type",
            response = UpdateCaseNoteType.class,
            notes = "Creates a new case note type")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The case note type has been updated. The updated object is returned.", response = CaseNoteType.class),
            @ApiResponse(code = 404, message = "The case note type is not found", response = ErrorResponse.class)})
    public CaseNoteType updateCaseNoteType(
            @ApiParam(value = "Parent Case Note Type", required = true, example = "OBS") @PathVariable("parentType") final String parentType,
            @RequestBody @NotNull final UpdateCaseNoteType body) {
        return caseNoteService.updateCaseNoteType(parentType, body);
    }

    @PutMapping(value = "/types/{parentType}/{subType}", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Update Case Note Sub Type",
            response = UpdateCaseNoteType.class,
            notes = "Creates a new case note sub type")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The case note sub type update has been updated. The updated object is returned.", response = CaseNoteType.class),
            @ApiResponse(code = 404, message = "The case note sub type is not found", response = ErrorResponse.class)})
    public CaseNoteType updateCaseNoteSubType(
            @ApiParam(value = "Parent Case Note Type", required = true, example = "OBS") @PathVariable("parentType") final String parentType,
            @ApiParam(value = "Sub Case Note Type", required = true, example = "GEN") @PathVariable("subType") final String subType,
            @RequestBody @NotNull final UpdateCaseNoteType body) {
        return caseNoteService.updateCaseNoteSubType(parentType, subType, body);
    }

    @GetMapping("/events_no_limit")
    @ApiIgnore
    @ApiOperation(value = "Fetch all case notes of a given type since a given date", notes = "Returns all case notes - consumed by Delius<br/>" +
            "These are generated by  whenever a case note is created or amended for an offender.<br/>" +
            "*Note:* An alternative call [GET /case_notes/for_delius](#case-notes-feed-fetch-case-notes-direct-get) has been created for performance reasons.<br/>" +
            "Some case notes are automatically generated by the system in response to an action relating to the offender e.g. An alert being raised or a prison transfer.<br/>" +
            "The note type only filters at the top note type level not the sub type.<br/>" +
            "note_type can be presented multiples times in the URL to filter by multiple note types.", nickname = "getCaseNotesEvents")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = CaseNoteEvents.class),
            @ApiResponse(code = 400, message = "Invalid request.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Unrecoverable error occurred whilst processing request.", response = ErrorResponse.class)})
    CaseNoteEvents getCaseNotesEventsNoLimit(@ApiParam(value = "a list of types and optionally subtypes (joined with +) to search.", example = "ACP+ASSESSMENT", required = true) @RequestParam("type") final List<String> noteTypes,
                                             @ApiParam(value = "Only case notes occurring on or after this date and time (ISO 8601 format without timezone e.g. YYYY-MM-DDTHH:MM:SS) will be considered.") @RequestParam("createdDate") @DateTimeFormat(iso = ISO.DATE_TIME) final LocalDateTime createdDate) {
        return caseNoteService.getCaseNoteEvents(noteTypes, createdDate);
    }

    @GetMapping("/events")
    @ApiOperation(value = "Fetch all case notes of a given type since a given date", notes = "Returns all case notes - consumed by Delius<br/>" +
            "These are generated by  whenever a case note is created or amended for an offender.<br/>" +
            "*Note:* An alternative call [GET /case_notes/for_delius](#case-notes-feed-fetch-case-notes-direct-get) has been created for performance reasons.<br/>" +
            "Some case notes are automatically generated by the system in response to an action relating to the offender e.g. An alert being raised or a prison transfer.<br/>" +
            "The note type only filters at the top note type level not the sub type.<br/>" +
            "note_type can be presented multiples times in the URL to filter by multiple note types.", nickname = "getCaseNotesEvents")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = CaseNoteEvents.class),
            @ApiResponse(code = 400, message = "Invalid request.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Unrecoverable error occurred whilst processing request.", response = ErrorResponse.class)})
    CaseNoteEvents getCaseNotesEvents(@ApiParam(value = "a list of types and optionally subtypes (joined with +) to search.", example = "ACP+ASSESSMENT", required = true) @RequestParam("type") final List<String> noteTypes,
                                      @ApiParam(value = "Only case notes occurring on or after this date and time (ISO 8601 format without timezone e.g. YYYY-MM-DDTHH:MM:SS) will be considered.") @RequestParam("createdDate") @DateTimeFormat(iso = ISO.DATE_TIME) final LocalDateTime createdDate,
                                      @ApiParam(name = "limit", value = "Number of events to return", example = "100", required = true) @RequestParam("limit") final Integer limit) {
        return caseNoteService.getCaseNoteEvents(noteTypes, createdDate, limit);
    }

    private Map<String, String> createEventProperties(final CaseNote caseNote) {
        return Map.of(
                "type", caseNote.getType(),
                "subType", caseNote.getSubType(),
                "offenderIdentifier", caseNote.getOffenderIdentifier(),
                "authorUsername", securityUserContext.getCurrentUsername()
        );
    }
}
