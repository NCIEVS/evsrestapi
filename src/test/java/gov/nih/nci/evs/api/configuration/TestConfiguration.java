package gov.nih.nci.evs.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gov.nih.nci.evs.api.properties.TestProperties;

@Configuration
@EnableConfigurationProperties({ TestProperties.class })
public class TestConfiguration {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(TestConfiguration.class);

    public TestConfiguration() {
        log.debug("Creating instance of class TestConfiguration");
    }

   

}
