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
import java.time.Duration.ofMinutes

class CaseNotesUsageSimulation : Simulation() {

  val personIdentifiers = csv("person-identifiers-${getenv("ENVIRONMENT_NAME")}.csv").random()
  val authorIds = csv("author-ids-${getenv("ENVIRONMENT_NAME")}.csv").random()
  val kwType = "KA"
  val kwSubType = "KS"

  fun usageByPersonIdentifier() = exec(
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

  fun usageByAuthorId() = exec(
    http("Usage by author id")
      .post("/case-notes/staff-usage")
      .body(
        StringBody {
          val authorId = it.getString("authorId")
          """
        {
          "authorIds": ["$authorId"],
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

  val usageByPi = scenario("usage by person identifier").exec(getToken)
    .repeat(1).on(feed(personIdentifiers), usageByPersonIdentifier())

  val usageByAuthorId = scenario("usage by author id").exec(getToken)
    .repeat(1).on(feed(authorIds), usageByAuthorId())

  init {
    setUp(
      usageByPi.injectClosed(constantConcurrentUsers(10).during(ofMinutes(10))),
      usageByAuthorId.injectClosed(constantConcurrentUsers(10).during(ofMinutes(10))),
    ).protocols(httpProtocol)
  }
}
