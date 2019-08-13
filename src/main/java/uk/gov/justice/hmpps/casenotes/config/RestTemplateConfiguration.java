package uk.gov.justice.hmpps.casenotes.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.RequestEnhancer;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.hmpps.casenotes.utils.JwtAuthInterceptor;
import uk.gov.justice.hmpps.casenotes.utils.W3cTracingInterceptor;

import java.util.List;

@Configuration
public class RestTemplateConfiguration {

    private final OffenderCaseNoteProperties properties;
    private final OAuth2ClientContext oauth2ClientContext;
    private final ClientCredentialsResourceDetails elite2apiDetails;

    public RestTemplateConfiguration(OffenderCaseNoteProperties properties, final OAuth2ClientContext oauth2ClientContext,
                                     final ClientCredentialsResourceDetails elite2apiDetails) {
        this.properties = properties;
        this.oauth2ClientContext = oauth2ClientContext;
        this.elite2apiDetails = elite2apiDetails;
    }

    @Bean(name = "elite2ApiRestTemplate")
    public RestTemplate elite2ApiRestTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return getRestTemplate(restTemplateBuilder, properties.getElite2ApiRestUrl());
    }

    @Bean(name = "oauthApiRestTemplate")
    public RestTemplate oauthApiRestTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return getRestTemplate(restTemplateBuilder, properties.getOauthApiRestUrl());
    }


    private RestTemplate getRestTemplate(final RestTemplateBuilder restTemplateBuilder, final String uri) {
        return restTemplateBuilder
                .rootUri(uri)
                .additionalInterceptors(getRequestInterceptors())
                .build();
    }

    private List<ClientHttpRequestInterceptor> getRequestInterceptors() {
        return List.of(
                new W3cTracingInterceptor(),
                new JwtAuthInterceptor());
    }

    @Bean
    public OAuth2RestTemplate elite2SystemRestTemplate(final SecurityUserContext authenticationFacade) {

        final var elite2SystemRestTemplate = new OAuth2RestTemplate(elite2apiDetails, oauth2ClientContext);
        final var systemInterceptors = elite2SystemRestTemplate.getInterceptors();

        systemInterceptors.add(new W3cTracingInterceptor());

        elite2SystemRestTemplate.setAccessTokenProvider(new GatewayAwareAccessTokenProvider(authenticationFacade));

        RootUriTemplateHandler.addTo(elite2SystemRestTemplate, properties.getElite2ApiRestUrl());

        return elite2SystemRestTemplate;
    }


    public class GatewayAwareAccessTokenProvider extends ClientCredentialsAccessTokenProvider {

        GatewayAwareAccessTokenProvider(final SecurityUserContext authenticationFacade) {
            this.setTokenRequestEnhancer(new TokenRequestEnhancer(authenticationFacade));
        }

        @Override
        public RestOperations getRestTemplate() {
            return super.getRestTemplate();
        }
    }

    public class TokenRequestEnhancer implements RequestEnhancer {

        private final SecurityUserContext authenticationFacade;

        TokenRequestEnhancer(SecurityUserContext authenticationFacade) {
            this.authenticationFacade = authenticationFacade;
        }

        @Override
        public void enhance(AccessTokenRequest request, OAuth2ProtectedResourceDetails resource, MultiValueMap<String, String> form, HttpHeaders headers) {
            form.set("username", authenticationFacade.getCurrentUsername());
        }
    }
}
