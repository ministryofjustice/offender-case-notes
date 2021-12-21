package uk.gov.justice.hmpps.casenotes.config

import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.util.ReflectionUtils
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider
import uk.gov.justice.hmpps.casenotes.controllers.CaseNoteController
import java.lang.reflect.Field
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Date
import java.util.Optional
import java.util.stream.Collectors

@Configuration
@Import(BeanValidatorPluginsConfiguration::class)
class SpringFoxConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Bean
  open fun api(): Docket {
    val apiInfo = ApiInfo(
      "HMPPS Offender Case Note Documentation",
      "API for Case note details for offenders.",
      this.version,
      "https://gateway.nomis-api.service.justice.gov.uk/auth/terms",
      contactInfo(),
      "Open Government Licence v3.0",
      "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/",
      emptyList()
    )

    val docket = Docket(DocumentationType.OAS_30)
      .useDefaultResponseMessages(false)
      .apiInfo(apiInfo)
      .select()
      .apis(RequestHandlerSelectors.basePackage(CaseNoteController::class.java.getPackage().getName()))
      .paths(PathSelectors.any())
      .build()
    docket.genericModelSubstitutes(Optional::class.java)
    docket.directModelSubstitute(ZonedDateTime::class.java, Date::class.java)
    docket.directModelSubstitute(LocalDateTime::class.java, Date::class.java)
    return docket
  }

  private fun contactInfo() = Contact(
    "HMPPS Digital Studio",
    "",
    "feedback@digital.justice.gov.uk"
  )

  @Bean
  fun springfoxHandlerProviderBeanPostProcessor(): BeanPostProcessor? {
    return object : BeanPostProcessor {
      @Throws(BeansException::class)
      override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is WebMvcRequestHandlerProvider || bean is WebFluxRequestHandlerProvider) {
          customizeSpringfoxHandlerMappings(getHandlerMappings(bean))
        }
        return bean
      }

      private fun <T : RequestMappingInfoHandlerMapping?> customizeSpringfoxHandlerMappings(mappings: MutableList<T>) {
        val copy = mappings.stream()
          .filter { mapping: T -> mapping?.patternParser == null }
          .collect(Collectors.toList())
        mappings.clear()
        mappings.addAll(copy)
      }

      private fun getHandlerMappings(bean: Any): MutableList<RequestMappingInfoHandlerMapping> {
        return try {
          val field: Field? = ReflectionUtils.findField(bean.javaClass, "handlerMappings")
          field?.isAccessible = true
          @Suppress("UNCHECKED_CAST")
          field?.get(bean) as MutableList<RequestMappingInfoHandlerMapping>
        } catch (e: IllegalArgumentException) {
          throw IllegalStateException(e)
        } catch (e: IllegalAccessException) {
          throw IllegalStateException(e)
        }
      }
    }
  }
}
