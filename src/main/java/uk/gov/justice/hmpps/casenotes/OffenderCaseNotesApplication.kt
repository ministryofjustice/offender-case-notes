package uk.gov.justice.hmpps.casenotes

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer

@SpringBootApplication
@EnableResourceServer
open class OffenderCaseNotesApplication

fun main(args: Array<String>) {
  runApplication<OffenderCaseNotesApplication>(*args)
}
