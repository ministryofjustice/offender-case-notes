package uk.gov.justice.hmpps.casenotes.services

import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.jms.core.JmsTemplate
import uk.gov.justice.hmpps.casenotes.dto.CaseNote
import java.time.LocalDateTime

@RunWith(MockitoJUnitRunner::class)
class CaseNoteAwsEventPusherTest {
  private val jmsTemplate: JmsTemplate = mock()

  private lateinit var service: CaseNoteAwsEventPusher

  @Before
  fun before() {
    service = CaseNoteAwsEventPusher(jmsTemplate)
  }

  @Test
  fun `send event`() {
    service.sendEvent(caseCaseNote())
    verify(jmsTemplate).convertAndSend(check<CaseNoteEvent> {
      assertThat(it).isEqualTo(CaseNoteEvent(
          eventType = "GEN-OSE",
          eventDatetime = LocalDateTime.parse("2019-03-04T10:11:12"),
          offenderIdDisplay = "A1234AC",
          agencyLocationId = "MDI",
          caseNoteId = "abcde"))
    })
  }

  private fun caseCaseNote(): CaseNote {
    return CaseNote.builder()
        .caseNoteId("abcde")
        .creationDateTime(LocalDateTime.parse("2019-03-04T10:11:12"))
        .occurrenceDateTime(LocalDateTime.parse("2018-02-03T10:11:12"))
        .locationId("MDI")
        .authorUsername("USER2")
        .authorUserId("some user")
        .authorName("Mickey Mouse")
        .offenderIdentifier("A1234AC")
        .type("GEN")
        .subType("OSE")
        .text("HELLO")
        .build()
  }
}
