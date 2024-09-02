package uk.gov.justice.hmpps.casenotes

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import uk.gov.justice.hmpps.casenotes.config.ServiceConfig

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(ServiceConfig::class)
class OffenderCaseNotesApplication

fun main(args: Array<String>) {
  runApplication<OffenderCaseNotesApplication>(*args)
}
