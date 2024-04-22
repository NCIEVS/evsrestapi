package gov.nih.nci.evs.api.configuration;

import gov.nih.nci.evs.api.properties.TestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** The Class TestConfiguration. */
@Configuration
@EnableConfigurationProperties({TestProperties.class})
public class TestConfiguration {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(TestConfiguration.class);

  /** Instantiates an empty {@link TestConfiguration}. */
  public TestConfiguration() {
    log.debug("Creating instance of class TestConfiguration");
  }
}
