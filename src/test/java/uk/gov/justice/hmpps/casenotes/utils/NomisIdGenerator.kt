package uk.gov.justice.hmpps.casenotes.utils

import java.util.concurrent.atomic.AtomicLong

object NomisIdGenerator {
  private val id = AtomicLong(1)
  private val letters = ('A'..'Z')
  private val numbers = (1111..9999)

  fun newId(): Long = id.getAndIncrement()
  fun prisonNumber(): String = "${letters.random()}${numbers.random()}${letters.random()}${letters.random()}"
}
