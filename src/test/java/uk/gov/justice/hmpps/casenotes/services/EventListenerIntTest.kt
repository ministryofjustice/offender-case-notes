package uk.gov.justice.hmpps.casenotes.services

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.hmpps.casenotes.health.IntegrationTest
import uk.gov.justice.hmpps.casenotes.health.wiremock.Elite2Extension.Companion.elite2Api
import uk.gov.justice.hmpps.casenotes.health.wiremock.OAuthExtension.Companion.oAuthApi
import uk.gov.justice.hmpps.casenotes.repository.OffenderCaseNoteRepository

@ActiveProfiles("noqueue")
class EventListenerIntTest : IntegrationTest() {
  @Autowired
  private lateinit var eventListener: EventListener

  @MockBean
  private lateinit var repository: OffenderCaseNoteRepository

  @Test
  fun `test handle merge events`() {
    oAuthApi.stubGrantToken()
    elite2Api.stubGetBookingBasicInfo(100001)
    elite2Api.stubGetBookingIdentifiers(100001)
    eventListener.handleEvents("booking-number-changed.json".readFile())

    verify(repository).updateOffenderIdentifier("A2345CD", "A5156DY")
    verify(repository).updateOffenderIdentifier("A1234BC", "A5156DY")
  }
}

private fun String.readFile(): String = EventListenerIntTest::class.java.getResource(this).readText()
