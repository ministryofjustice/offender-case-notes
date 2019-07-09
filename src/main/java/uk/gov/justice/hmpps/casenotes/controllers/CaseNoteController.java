package uk.gov.justice.hmpps.casenotes.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"case-notes"})
@RestController
@RequestMapping(
        value="case-notes",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class CaseNoteController {

    @GetMapping("/{offenderIdentifier}")
    @ApiOperation(  value = "Returns list of case note for this offender",
                    response = String.class,
                    notes = "More Information Here")
    public List<String> getCaseNotesByOffenderIdentifier(
            @ApiParam(value = "Offender Identifier", required = true, example = "A1234AA") @PathVariable("offenderIdentifier") final String offenderIdentifier) {
        return List.of(offenderIdentifier, "HELLO", "GOODBYE");
    }
}
