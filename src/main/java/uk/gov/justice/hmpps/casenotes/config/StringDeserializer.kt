package uk.gov.justice.hmpps.casenotes.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException

@JsonComponent
class StringDeserializer : JsonDeserializer<String?>() {
  @Throws(IOException::class, JsonProcessingException::class)
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String? = p.text.filter { it != Char.MIN_VALUE }
}
