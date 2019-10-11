package uk.gov.justice.hmpps.casenotes.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthInterceptor;
import uk.gov.justice.hmpps.casenotes.utils.W3cTracingInterceptor;

import java.util.List;

@Configuration
public class RestTemplateConfiguration {

    private final OffenderCaseNoteProperties properties;

    public RestTemplateConfiguration(final OffenderCaseNoteProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "elite2ApiRestTemplate")
    public RestTemplate elite2ApiRestTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return getRestTemplate(restTemplateBuilder, properties.getElite2ApiBaseUrl());
    }

    @Bean(name = "oauthApiRestTemplate")
    public RestTemplate oauthApiRestTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return getRestTemplate(restTemplateBuilder, properties.getOauthApiBaseUrl());
    }

    @Bean(name = "elite2ApiRestTemplateWithTimeout")
    public RestTemplate elite2ApiRestTemplateWithTimeout(final RestTemplateBuilder restTemplateBuilder) {
        return getRestTemplateWithTimeout(restTemplateBuilder, properties.getElite2ApiBaseUrl());
    }

    @Bean(name = "oauthApiRestTemplateWithTimeout")
    public RestTemplate oauthApiRestTemplateWithTimeout(final RestTemplateBuilder restTemplateBuilder) {
        return getRestTemplateWithTimeout(restTemplateBuilder, properties.getOauthApiBaseUrl());
    }


    private RestTemplate getRestTemplate(final RestTemplateBuilder restTemplateBuilder, final String uri) {
        return restTemplateBuilder
                .rootUri(uri)
                .additionalInterceptors(getRequestInterceptors())
                .build();
    }

    private RestTemplate getRestTemplateWithTimeout(final RestTemplateBuilder restTemplateBuilder, final String uri) {
        return restTemplateBuilder
                .rootUri(uri)
                .additionalInterceptors(getRequestInterceptors())
                .setConnectTimeout(properties.getTimeout())
                .setReadTimeout(properties.getTimeout())
                .build();
    }

    private List<ClientHttpRequestInterceptor> getRequestInterceptors() {
        return List.of(
                new W3cTracingInterceptor(),
                new JwtAuthInterceptor());
    }
}
