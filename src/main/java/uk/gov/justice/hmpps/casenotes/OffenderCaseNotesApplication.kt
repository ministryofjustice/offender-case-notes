package uk.gov.justice.hmpps.casenotes

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class OffenderCaseNotesApplication

fun main(args: Array<String>) {
  runApplication<OffenderCaseNotesApplication>(*args)
}
