package uk.gov.justice.hmpps.casenotes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import uk.gov.justice.hmpps.casenotes.config.OffenderCaseNoteProperties;

@SpringBootApplication
@EnableConfigurationProperties(OffenderCaseNoteProperties.class)
@EnableResourceServer
public class OffenderCaseNotesApplication {
    public static void main(final String[] args) {
        SpringApplication.run(OffenderCaseNotesApplication.class, args);
    }

}
