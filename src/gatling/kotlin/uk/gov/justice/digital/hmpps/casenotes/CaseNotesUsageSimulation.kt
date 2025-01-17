package uk.gov.justice.digital.hmpps.casenotes

import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.csv
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.feed
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.lang.System.getenv
import java.time.Duration.ofMillis
import java.time.Duration.ofMinutes
import java.time.Duration.ofSeconds
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

class CaseNotesUsageSimulation : Simulation() {

  private val personIdentifiers = csv("person-identifiers-${getenv("ENVIRONMENT_NAME")}.csv").random()
  private val authorIds = csv("author-ids-${getenv("ENVIRONMENT_NAME")}.csv").random()
  private val prisonCodes = csv("prison-codes-${getenv("ENVIRONMENT_NAME")}.csv").random()
  private val kwType = "KA"
  private val kwSubType = "KS"

  private fun usageByPersonIdentifier() = exec(
    http("Usage by person identifier")
      .post("/case-notes/usage")
      .body(
        StringBody {
          val pi = it.getString("personIdentifier")
          """
        {
          "personIdentifiers": ["$pi"],
          "typeSubTypes": [
            {
              "type": "$kwType",
              "subTypes": ["$kwSubType"]
            }
          ]
        }
          """.trimIndent()
        },
      ).asJson()
      .headers(authorisationHeader)
      .check(status().shouldBe(200)),
  )

  private fun usageByAuthorId() = exec(
    http("Usage by author id")
      .post("/case-notes/staff-usage")
      .body(
        StringBody {
          val authorId = it.getString("authorId")
          val today = LocalDateTime.now()
          """
        {
          "authorIds": ["$authorId"],
          "typeSubTypes": [
            {
              "type": "$kwType",
              "subTypes": ["$kwSubType"],
              "occurredFrom": "${today.minusDays(30).format(ISO_LOCAL_DATE_TIME)}",
              "occurredTo": "${today.format(ISO_LOCAL_DATE_TIME)}"
            }
          ]
        }
          """.trimIndent()
        },
      ).asJson()
      .headers(authorisationHeader)
      .check(status().shouldBe(200)),
  )

  private fun usageByPrisonCode() = exec(
    http("Usage by prison code")
      .post("/case-notes/prison-usage")
      .body(
        StringBody {
          val prisonCode = it.getString("prisonCode")
          val today = LocalDateTime.now()
          """
        {
          "prisonCodes": ["$prisonCode"],
          "typeSubTypes": [
            {
              "type": "$kwType",
              "occurredFrom": "${today.minusDays(30).format(ISO_LOCAL_DATE_TIME)}",
              "occurredTo": "${today.format(ISO_LOCAL_DATE_TIME)}"
            }
          ]
        }
          """.trimIndent()
        },
      ).asJson()
      .headers(authorisationHeader)
      .check(status().shouldBe(200)),
  )

  private val usageByPi = scenario("usage by person identifier").exec(getToken)
    .repeat(10).on(feed(personIdentifiers), usageByPersonIdentifier().pause(ofMillis(100)))

  private val usageByAuthorId = scenario("usage by author id").exec(getToken)
    .repeat(10).on(feed(authorIds), usageByAuthorId().pause(ofMillis(300)))

  private val usageByPrisonCode = scenario("usage by prison code").exec(getToken)
    .repeat(4).on(feed(prisonCodes), usageByPrisonCode().pause(ofSeconds(10)))

  init {
    setUp(
      usageByPi.injectClosed(constantConcurrentUsers(10).during(ofMinutes(10))),
      usageByAuthorId.injectClosed(constantConcurrentUsers(10).during(ofMinutes(10))),
      usageByPrisonCode.injectClosed(constantConcurrentUsers(4).during(ofMinutes(10))),
    ).protocols(httpProtocol)
  }
}
