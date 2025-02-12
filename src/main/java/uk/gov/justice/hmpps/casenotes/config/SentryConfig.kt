package uk.gov.justice.hmpps.casenotes.config

import io.sentry.SentryOptions
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.regex.Pattern.matches

@Configuration
class SentryConfig {
  @Bean
  fun ignoreHealthRequests() = SentryOptions.BeforeSendTransactionCallback { transaction, _ ->
    transaction.transaction?.let { if (it.startsWith("GET /health") or it.startsWith("GET /info")) null else transaction }
  }

  @Bean
  fun transactionSampling() = SentryOptions.TracesSamplerCallback { context ->
    context.customSamplingContext?.let {
      val request = it["request"] as HttpServletRequest
      when (request.method) {
        "GET" if (request.requestURI.isHighUsage()) -> {
          0.005
        }

        else -> {
          0.01
        }
      }
    }
  }

  private fun String.isHighUsage(): Boolean = this == "/case-notes/types" || matches("(/sync)?/case-notes/[A-Z][0-9]{4}[A-Z]{2}", this)
}
