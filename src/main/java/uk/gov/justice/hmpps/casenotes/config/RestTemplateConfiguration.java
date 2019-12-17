package uk.gov.justice.hmpps.casenotes.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthInterceptor;

import java.util.List;

@Configuration
public class RestTemplateConfiguration {

    private final OffenderCaseNoteProperties properties;
    private final ClientCredentialsResourceDetails offenderCaseNotesClientCredentials;

    public RestTemplateConfiguration(final OffenderCaseNoteProperties properties, ClientCredentialsResourceDetails offenderCaseNotesClientCredentials) {
        this.properties = properties;
        this.offenderCaseNotesClientCredentials = offenderCaseNotesClientCredentials;
    }

    @Bean(name = "elite2ApiRestTemplate")
    public RestTemplate elite2ApiRestTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return getRestTemplate(restTemplateBuilder, properties.getElite2ApiBaseUrl());
    }

    @Bean(name = "oauthApiRestTemplate")
    public RestTemplate oauthApiRestTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return getRestTemplate(restTemplateBuilder, properties.getOauthApiBaseUrl());
    }

    @Bean(name = "elite2ApiHealthRestTemplate")
    public RestTemplate elite2ApiHealthRestTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return getHealthRestTemplate(restTemplateBuilder, properties.getElite2ApiBaseUrl());
    }

    @Bean(name = "oauthApiHealthRestTemplate")
    public RestTemplate oauthApiRestHealthTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return getHealthRestTemplate(restTemplateBuilder, properties.getOauthApiBaseUrl());
    }

    @Bean(name = "clientCredentialsRestTemplate")
    public OAuth2RestTemplate clientCredentialsRestTemplate() {
        final var systemRestTemplate = new OAuth2RestTemplate(offenderCaseNotesClientCredentials);
        RootUriTemplateHandler.addTo(systemRestTemplate, properties.getElite2ApiBaseUrl());
        return systemRestTemplate;
    }

    private RestTemplate getRestTemplate(final RestTemplateBuilder restTemplateBuilder, final String uri) {
        return restTemplateBuilder
                .rootUri(uri)
                .additionalInterceptors(getRequestInterceptors())
                .build();
    }

    private RestTemplate getHealthRestTemplate(final RestTemplateBuilder restTemplateBuilder, final String uri) {
        return restTemplateBuilder
                .rootUri(uri)
                .additionalInterceptors(getRequestInterceptors())
                .setConnectTimeout(properties.getHealthTimeout())
                .setReadTimeout(properties.getHealthTimeout())
                .build();
    }

    private List<ClientHttpRequestInterceptor> getRequestInterceptors() {
        return List.of(new JwtAuthInterceptor());
    }
}
