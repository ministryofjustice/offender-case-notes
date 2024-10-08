package uk.gov.justice.hmpps.casenotes.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import wiremock.org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

@JsonTest
class EventListenerTest(@Autowired objectMapper: ObjectMapper) {
  private val mergeOffenderService: MergeOffenderService = mock()
  private var eventListener: EventListener = EventListener(mergeOffenderService, objectMapper)

  @Test
  fun testMergeEvent() {
    whenever(mergeOffenderService.checkAndMerge(any())).thenReturn(2)
    eventListener.handleEvents(getJson("booking-number-changed.json"))
    verify(mergeOffenderService).checkAndMerge(100001L)
  }

  private fun getJson(filename: String): String =
    IOUtils.toString(this::class.java.getResourceAsStream(filename), StandardCharsets.UTF_8.toString())
}
