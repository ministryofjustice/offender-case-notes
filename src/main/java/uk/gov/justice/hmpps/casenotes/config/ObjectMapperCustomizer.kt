package uk.gov.justice.hmpps.casenotes.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.cfg.AvailableSettings
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ObjectMapperCustomizer {

  // Configure hibernate to use the same Object Mapper as Spring
  // TODO This is still using Jackson 2 as Hibernate does not fully support Jackson 3 yet - see https://github.com/hibernate/hibernate-orm/pull/11357#issuecomment-3773520899
  @Bean
  fun jsonFormatMapperCustomizer(objectMapper: ObjectMapper): HibernatePropertiesCustomizer = HibernatePropertiesCustomizer { properties: MutableMap<String, Any> ->
    properties[AvailableSettings.JSON_FORMAT_MAPPER] = JacksonJsonFormatMapper(objectMapper)
  }
}
