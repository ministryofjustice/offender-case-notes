package uk.gov.justice.hmpps.casenotes.domain

import com.fasterxml.uuid.Generators
import java.util.UUID

object IdGenerator {
  fun newUuid(): UUID {
    return Generators.timeBasedEpochGenerator().generate()
  }
}
