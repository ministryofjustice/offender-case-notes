package uk.gov.justice.hmpps.casenotes.schemaspy

import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.casenotes.controllers.IntegrationTest

class SchemaSpyIntTest : IntegrationTest() {

  @Test
  fun `initialises database`() {
    println("Database has been initialised by IntegrationTest")
  }
}