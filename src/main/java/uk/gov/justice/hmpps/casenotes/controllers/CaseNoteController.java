package uk.gov.justice.hmpps.casenotes.controllers;

import com.microsoft.applicationinsights.TelemetryClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.hmpps.casenotes.config.SecurityUserContext;
import uk.gov.justice.hmpps.casenotes.dto.CaseNote;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteFilter;
import uk.gov.justice.hmpps.casenotes.dto.CaseNoteTypeDto;
import uk.gov.justice.hmpps.casenotes.dto.ErrorResponse;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.NewCaseNoteType;
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNote;
import uk.gov.justice.hmpps.casenotes.dto.UpdateCaseNoteType;
import uk.gov.justice.hmpps.casenotes.services.CaseNoteEventPusher;
import uk.gov.justice.hmpps.casenotes.services.CaseNoteService;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "case-notes", description = "Case Note Controller")
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
    private final CaseNoteEventPusher caseNoteEventPusher;

    @GetMapping("/{offenderIdentifier}/{caseNoteIdentifier}")
    @ResponseBody
    @Operation(summary = "Retrieves a case note")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Offender or case note not found",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
        })
    public CaseNote getCaseNote(
        @Parameter(description = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
        @Parameter(description = "Case Note Id", required = true, example = "518b2200-6489-4c77-8514-10cf80ccd488") @PathVariable("caseNoteIdentifier") final String caseNoteIdentifier) {
        return caseNoteService.getCaseNote(offenderIdentifier, caseNoteIdentifier);
    }

    @GetMapping("/{offenderIdentifier}")
    @ResponseBody
    @Operation(summary = "Retrieves a list of case notes")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Offender not found",
        content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
    })
    public Page<CaseNote> getCaseNotes(
        @Parameter(description = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
        @Parameter(description = "Optionally specify a case note filter") final CaseNoteFilter filter,
        @PageableDefault(sort = {"occurrenceDateTime"}, direction = Sort.Direction.DESC) final Pageable pageable) {
        return caseNoteService.getCaseNotes(offenderIdentifier, filter, pageable);
    }

    @PostMapping(value = "/{offenderIdentifier}", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add Case Note for offender", description = "Creates a note for a specific type/subType")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "The Case Note has been recorded. The updated object is returned including the status."),
        @ApiResponse(responseCode = "409", description = "The case note has already been recorded under the booking. The current unmodified object (including status) is returned.",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
        })
    public CaseNote createCaseNote(
        @Parameter(description = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
        @RequestBody @NotNull final NewCaseNote newCaseNote) {
        final var caseNoteCreated = caseNoteService.createCaseNote(offenderIdentifier, newCaseNote);
        // Log event
        telemetryClient.trackEvent("CaseNoteCreated", createEventProperties(caseNoteCreated), null);
        // and push event to offender events topic
        caseNoteEventPusher.sendEvent(caseNoteCreated);
        return caseNoteCreated;
    }

    @PutMapping(value = "/{offenderIdentifier}/{caseNoteIdentifier}", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Amend Case Note for offender", description = "Amend a case note information adds and additional entry to the note")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "The Case Note has been recorded. The updated object is returned including the status."),
        @ApiResponse(responseCode = "404", description = "No case notes where found for this offender and case note id",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
        })
    public CaseNote amendCaseNote(
        @Parameter(description = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
        @Parameter(description = "Case Note Id", required = true, example = "518b2200-6489-4c77-8514-10cf80ccd488") @PathVariable("caseNoteIdentifier") final String caseNoteIdentifier,
        @RequestBody @NotNull final UpdateCaseNote amendedText) {
        final var amendCaseNote = caseNoteService.amendCaseNote(offenderIdentifier, caseNoteIdentifier, amendedText);

        // Log event
        telemetryClient.trackEvent("CaseNoteUpdated", createEventProperties(amendCaseNote), null);
        // and push event to offender events topic
        caseNoteEventPusher.sendEvent(amendCaseNote);
        return amendCaseNote;
    }

    @GetMapping(value = "/types")
    @Operation(summary = "Retrieves a list of case note types")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Case notes types not found",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
        })
    public List<CaseNoteTypeDto> getCaseNoteTypes() {
        return caseNoteService.getCaseNoteTypes();
    }

    @GetMapping("/types-for-user")
    @Operation(summary = "Retrieves a list of case note types for this user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Case notes types not found",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
        })
    public List<CaseNoteTypeDto> getUserCaseNoteTypes() {
        return caseNoteService.getUserCaseNoteTypes();

    }

    @PostMapping(value = "/types", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add New Case Note Type", description = "Creates a new case note type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "The Case Note Type has been recorded. The updated object is returned including the status."),
        @ApiResponse(responseCode = "409", description = "The case note type has already been recorded. The current unmodified object (including status) is returned.",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
    })
    public CaseNoteTypeDto createCaseNoteType(@RequestBody @NotNull final NewCaseNoteType body) {
        return caseNoteService.createCaseNoteType(body);
    }

    @PostMapping(value = "/types/{parentType}", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add New Case Note Sub Type", description = "Creates a new case note sub type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "The Case Note Sub Type has been recorded. The updated object is returned including the status."),
        @ApiResponse(responseCode = "409", description = "The case note sub type has already been recorded. The current unmodified object (including status) is returned.",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
    })
    public CaseNoteTypeDto createCaseNoteSubType(
        @Parameter(description = "Parent Case Note Type", required = true, example = "GEN") @PathVariable("parentType") final String parentType,
        @RequestBody @NotNull final NewCaseNoteType body) {
        return caseNoteService.createCaseNoteSubType(parentType, body);
    }

    @PutMapping(value = "/types/{parentType}", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update Case Note Type", description = "Creates a new case note type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The case note type has been updated. The updated object is returned."),
        @ApiResponse(responseCode = "404", description = "The case note type is not found",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
    })
    public CaseNoteTypeDto updateCaseNoteType(
        @Parameter(description = "Parent Case Note Type", required = true, example = "OBS") @PathVariable("parentType") final String parentType,
        @RequestBody @NotNull final UpdateCaseNoteType body) {
        return caseNoteService.updateCaseNoteType(parentType, body);
    }

    @PutMapping(value = "/types/{parentType}/{subType}", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update Case Note Sub Type",
        description = "Creates a new case note sub type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The case note sub type update has been updated. The updated object is returned."),
        @ApiResponse(responseCode = "404", description = "The case note sub type is not found",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
    })
    public CaseNoteTypeDto updateCaseNoteSubType(
        @Parameter(description = "Parent Case Note Type", required = true, example = "OBS") @PathVariable("parentType") final String parentType,
        @Parameter(description = "Sub Case Note Type", required = true, example = "GEN") @PathVariable("subType") final String subType,
        @RequestBody @NotNull final UpdateCaseNoteType body) {
        return caseNoteService.updateCaseNoteSubType(parentType, subType, body);
    }

    @DeleteMapping("/{offenderIdentifier}/{caseNoteId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Deletes a case note")
    @ApiResponses({
          @ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "404", description = "Offender or case note not found",
              content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
    })
    public void softDeleteCaseNote(
        @Parameter(description = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
        @Parameter(description = "Case Note Id", required = true, example = "518b2200-6489-4c77-8514-10cf80ccd488") @PathVariable("caseNoteId") final String caseNoteId) {
        caseNoteService.softDeleteCaseNote(offenderIdentifier, caseNoteId);
    }

    @DeleteMapping("/amendment/{offenderIdentifier}/{caseNoteAmendmentId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Deletes a case note amendment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Offender or case note not found",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))}
        )})
    public void softDeleteCaseNoteAmendment(
        @Parameter(description = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier,
        @Parameter(description = "Case Note Amendment Id", required = true, example = "1") @PathVariable("caseNoteAmendmentId") final Long caseNoteAmendmentId) {
        caseNoteService.softDeleteCaseNoteAmendment(offenderIdentifier, caseNoteAmendmentId);
    }

    private Map<String, String> createEventProperties(final CaseNote caseNote) {
        return Map.of(
            "caseNoteId", caseNote.getCaseNoteId(),
            "caseNoteType", String.format("%s-%s", caseNote.getType(), caseNote.getSubType()),
            "type", caseNote.getType(),
            "subType", caseNote.getSubType(),
            "offenderIdentifier", caseNote.getOffenderIdentifier(),
            "authorUsername", securityUserContext.getCurrentUsername().orElse("unknown")
        );
    }
}
