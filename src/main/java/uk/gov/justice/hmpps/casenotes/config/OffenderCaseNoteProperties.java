package uk.gov.justice.hmpps.casenotes.config;

import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(value = "app")
@Data
@Validated
public class OffenderCaseNoteProperties {

    /**
     * Elite2 API Base URL endpoint ("http://localhost:8080")
     */
    @URL
    private String elite2ApiBaseUrl;

    /**
     * Elite2 API Rest URL endpoint ("http://localhost:8080/api")
     */
    @URL
    private String elite2ApiRestUrl;

    /**
     * OAUTH2 API Rest URL endpoint ("http://localhost:9090/auth/api")
     */
    @URL
    private String oauthApiRestUrl;

    /**
     * JWT Public Key
     */
    private String jwtPublicKey;
}
