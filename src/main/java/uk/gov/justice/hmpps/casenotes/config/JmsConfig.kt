package uk.gov.justice.hmpps.casenotes.config

import com.amazon.sqs.javamessaging.ProviderConfiguration
import com.amazon.sqs.javamessaging.SQSConnectionFactory
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.core.JmsTemplate


@Configuration
@EnableJms
open class JmsConfig {
  @Bean
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @ConditionalOnProperty(name = ["sqs.provider"])
  open fun awsJmsTemplate(awsSqs: AmazonSQS, @Value("\${sqs.topic.name}") topicName: String?): JmsTemplate {
    val jms = JmsTemplate(SQSConnectionFactory(ProviderConfiguration(), awsSqs))
    jms.isSessionTransacted = true
    jms.defaultDestinationName = topicName
    return jms
  }

  @Bean
  @ConditionalOnProperty(name = ["sqs.provider"], havingValue = "aws")
  open fun awsSqsClient(@Value("\${sqs.aws.access.key.id}") accessKey: String?,
                        @Value("\${sqs.aws.secret.access.key}") secretKey: String?,
                        @Value("\${sqs.endpoint.region}") region: String?): AmazonSQS =
      AmazonSQSAsyncClientBuilder.standard()
          .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKey, secretKey)))
          .withRegion(region)
          .build()

  @Bean
  @ConditionalOnProperty(name = ["sqs.provider"], havingValue = "localstack")
  open fun awsLocalClient(@Value("\${sqs.endpoint.url}") serviceEndpoint: String?,
                          @Value("\${sqs.endpoint.region}") region: String?): AmazonSQS =
      AmazonSQSAsyncClientBuilder.standard()
          .withEndpointConfiguration(EndpointConfiguration(serviceEndpoint, region))
          .build()
}
