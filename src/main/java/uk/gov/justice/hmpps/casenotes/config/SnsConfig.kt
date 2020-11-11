package uk.gov.justice.hmpps.casenotes.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import java.net.URI

@Configuration
open class SnsConfig {
  @Bean
  @ConditionalOnProperty(name = ["sns.provider"], havingValue = "aws")
  open fun awsSnsClient(
    @Value("\${sns.aws.access.key.id}") accessKey: String,
    @Value("\${sns.aws.secret.access.key}") secretKey: String,
    @Value("\${sns.endpoint.region}") region: String
  ): SnsAsyncClient =
    SnsAsyncClient.builder()
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
      .region(Region.of(region))
      .build()

  @Bean
  @ConditionalOnProperty(name = ["sns.provider"], havingValue = "localstack")
  open fun awsLocalClient(
    @Value("\${sns.endpoint.url}") serviceEndpoint: String,
    @Value("\${sns.endpoint.region}") region: String
  ): SnsAsyncClient =
    SnsAsyncClient.builder()
      .endpointOverride(URI.create(serviceEndpoint))
      .region(Region.of(region))
      .build()
}
