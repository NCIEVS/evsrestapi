package gov.nih.nci.evs.api.configuration;

import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.properties.GraphProperties;
import gov.nih.nci.evs.api.properties.OpensearchQueryProperties;
import gov.nih.nci.evs.api.properties.OpensearchServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Properties configuration. */
@Configuration
@EnableConfigurationProperties
public class PropertiesConfiguration {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(PropertiesConfiguration.class);

  /** Instantiates an empty {@link PropertiesConfiguration}. */
  public PropertiesConfiguration() {
    log.debug("Creating instance of class PropertiesConfiguration");
  }

  /**
   * Server properties.
   *
   * @return the server properties
   */
  @Bean
  @Primary
  ServerProperties serverProperties() {
    return new ServerProperties();
  }

  /**
   * Graph db properties.
   *
   * @return the graph db properties
   */
  @Bean
  @ConfigurationProperties(prefix = "nci.evs.graph", ignoreUnknownFields = false)
  GraphProperties graphProperties() {
    return new GraphProperties();
  }

  /**
   * Application properties.
   *
   * @return the application properties
   */
  @Bean
  @ConfigurationProperties(prefix = "nci.evs.application", ignoreUnknownFields = false)
  ApplicationProperties applicationProperties() {
    return new ApplicationProperties();
  }

  /**
   * Opensearch query properties.
   *
   * @return the opensearch query properties
   */
  @Bean
  @ConfigurationProperties(prefix = "nci.evs.elasticsearch.query", ignoreUnknownFields = false)
  OpensearchQueryProperties OpensearchQueryProperties() {
    return new OpensearchQueryProperties();
  }

  /**
   * Opensearch server properties.
   *
   * @return the opensearch server properties
   */
  @Bean
  @ConfigurationProperties(prefix = "nci.evs.elasticsearch.server", ignoreUnknownFields = false)
  OpensearchServerProperties OpensearchServerProperties() {
    return new OpensearchServerProperties();
  }
}
