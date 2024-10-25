package uk.gov.justice.digital.hmpps.casenotes

import io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.csv
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.feed
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.rampUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.lang.System.getenv
import java.time.Duration.ofMinutes
import java.time.Duration.ofSeconds

class CaseNotesSimulation : Simulation() {

  val personIdentifiers = csv("person-identifiers-${getenv("ENVIRONMENT_NAME")}.csv").random()

  val httpProtocol = http.baseUrl(getenv("BASE_URL"))
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate")

  val authorisationHeader = mapOf("authorization" to "Bearer #{authToken}")

  val getToken = exec(
    exec { session -> getenv("AUTH_TOKEN")?.let { session.set("authToken", it) } ?: session }
      .doIf { it.getString("authToken").isNullOrBlank() }
      .then(
        http("Get Auth Token")
          .post(getenv("AUTH_URL"))
          .queryParam("grant_type", "client_credentials")
          .basicAuth(getenv("CLIENT_ID"), getenv("CLIENT_SECRET"))
          .check(status().shouldBe(200), jsonPath("$.access_token").exists().saveAs("authToken")),
      ),
  )

  fun listCaseNotes(pageSize: Int) = exec(
    http("Find Case Notes page 1")
      .get("/case-notes/#{personIdentifier}")
      .queryParam("size", pageSize)
      .headers(authorisationHeader)
      .header("CaseloadId", "LEI")
      .check(status().shouldBe(200))
      .check(jsonPath("$.totalPages").exists().saveAs("totalPages")),
  ).exec {
    it.set("totalPages", "#{totalPages}")
    it.set("currentPage", 1)
  }.repeat { it.getString("totalPages")?.toInt()?.minus(1) ?: 0 }.on(
    exec { it.set("currentPage", it.getInt("currentPage") + 1) }
      .exec(
        http("Find case notes page #{currentPage} / #{totalPages}")
          .get("/case-notes/#{personIdentifier}")
          .queryParam("size", pageSize)
          .queryParam("page", "#{currentPage}")
          .headers(authorisationHeader)
          .header("CaseloadId", "LEI")
          .check(status().shouldBe(200)),
      ),
  )

  val paginated = scenario("paginated").exec(getToken)
    .repeat(10).on(feed(personIdentifiers), listCaseNotes(20))
  val unpaginated = scenario("unpaginated").exec(getToken)
    .repeat(10).on(feed(personIdentifiers), listCaseNotes(9999))

  init {
    setUp(
      paginated.injectClosed(constantConcurrentUsers(30).during(ofMinutes(10))),
      unpaginated.injectClosed(constantConcurrentUsers(10).during(ofMinutes(10))),
    ).protocols(httpProtocol)
  }
}
