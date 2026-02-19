package uk.gov.justice.hmpps.casenotes.utils

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.KotlinModule
import uk.gov.justice.hmpps.casenotes.config.ZonedDateTimeDeserializer
import java.time.ZonedDateTime

object JsonHelper {
  val jsonMapper: JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .addModule(SimpleModule().addDeserializer(ZonedDateTime::class.java, ZonedDateTimeDeserializer()))
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .enable(SerializationFeature.INDENT_OUTPUT)
    .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
    .changeDefaultPropertyInclusion { it.withContentInclusion(JsonInclude.Include.NON_NULL) }
    .build()
}
