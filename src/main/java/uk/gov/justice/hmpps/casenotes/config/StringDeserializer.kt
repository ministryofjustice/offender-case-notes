package uk.gov.justice.hmpps.casenotes.config

import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JacksonException
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import java.io.IOException

@JacksonComponent
class StringDeserializer : ValueDeserializer<String?>() {
  @Throws(IOException::class, JacksonException::class)
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String? = p.string.filter { it != Char.MIN_VALUE }
}
