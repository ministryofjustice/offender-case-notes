package uk.gov.justice.hmpps.casenotes.services

import com.google.gson.GsonBuilder
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import wiremock.org.apache.commons.io.IOUtils
import java.io.IOException
import java.nio.charset.StandardCharsets

@ExtendWith(MockitoExtension::class)
class EventListenerTest {
  private val caseNoteService: CaseNoteService = mock()

  private val mergeOffenderService: MergeOffenderService = mock()
  private lateinit var eventListener: EventListener

  @BeforeEach
  fun setup() {
    eventListener = EventListener(caseNoteService, mergeOffenderService, GsonBuilder().create())
  }

  @Test
  @Throws(IOException::class)
  fun testDeleteEvent() {
    whenever(caseNoteService.deleteCaseNotesForOffender(eq("A1234AA"))).thenReturn(3)
    eventListener.handleEvents(getJson("offender-deletion-request.json"))
    verify(caseNoteService).deleteCaseNotesForOffender(eq("A1234AA"))
  }

  @Test
  @Throws(IOException::class)
  fun testMergeEvent() {
    whenever(mergeOffenderService.checkAndMerge(eq(100001L))).thenReturn(2)
    eventListener.handleEvents(getJson("booking-number-changed.json"))
    verify(mergeOffenderService).checkAndMerge(eq(100001L))
  }

  @Throws(IOException::class)
  private fun getJson(filename: String): String {
    return IOUtils.toString(javaClass.getResourceAsStream(filename), StandardCharsets.UTF_8.toString())
  }
}
