package uk.gov.justice.hmpps.casenotes.config

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.localstack.LocalStackContainer


@Configuration
@ConditionalOnExpression("'\${sqs.provider}'.equals('localstack') and '\${sqs.embedded}'.equals('true')")
open class JmsLocalStackConfig(private val localStackContainer: LocalStackContainer) {

  @Bean
  open fun awsSqsClient(): AmazonSQS {
    return AmazonSQSClientBuilder.standard()
        .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
        .withCredentials(localStackContainer.defaultCredentialsProvider)
        .build()
  }

  @Bean
  open fun awsSqsDlqClient(): AmazonSQS {
    return AmazonSQSClientBuilder.standard()
        .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
        .withCredentials(localStackContainer.defaultCredentialsProvider)
        .build()
  }
}
