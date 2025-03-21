package uk.gov.justice.hmpps.casenotes.alertnotes

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDate.now

@RestController
@RequestMapping
class AlertCaseNoteReconciliationController(private val reconciliationEventGenerator: ReconciliationEventGenerator) {
  @Operation(hidden = true)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/case-notes/alerts/reconciliation")
  fun caseNoteReconciliation(
    @RequestParam(required = false) from: LocalDate?,
    @RequestParam(required = false) to: LocalDate?,
  ) {
    reconciliationEventGenerator.generateEventsFor(from ?: now().minusDays(1), to ?: now())
  }
}
