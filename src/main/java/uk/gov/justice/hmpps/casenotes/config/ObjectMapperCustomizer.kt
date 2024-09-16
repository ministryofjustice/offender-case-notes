package uk.gov.justice.hmpps.casenotes.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.cfg.AvailableSettings
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ObjectMapperCustomizer {

  // Configure hibernate to use the same Object Mapper as Spring
  @Bean
  fun jsonFormatMapperCustomizer(objectMapper: ObjectMapper): HibernatePropertiesCustomizer {
    return HibernatePropertiesCustomizer { properties: MutableMap<String, Any> ->
      properties[AvailableSettings.JSON_FORMAT_MAPPER] = JacksonJsonFormatMapper(objectMapper)
    }
  }
}
