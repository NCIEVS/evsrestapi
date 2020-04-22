
package gov.nih.nci.evs.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.properties.ElasticQueryProperties;
import gov.nih.nci.evs.api.properties.ElasticServerProperties;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.properties.SwaggerProperties;
import gov.nih.nci.evs.api.properties.ThesaurusProperties;

/**
 * Properties configuration.
 */
@Configuration
@EnableConfigurationProperties
public class PropertiesConfiguration {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(PropertiesConfiguration.class);

  /**
   * Instantiates an empty {@link PropertiesConfiguration}.
   */
  public PropertiesConfiguration() {
    log.debug("Creating instance of class PropertiesConfiguration");
  }

  /**
   * Server properties.
   *
   * @return the server properties
   */
  @Bean
  ServerProperties serverProperties() {
    return new ServerProperties();
  }

  /**
   * Stardog properties.
   *
   * @return the stardog properties
   */
  @Bean
  @ConfigurationProperties(prefix = "nci.evs.stardog", ignoreUnknownFields = false)
  StardogProperties stardogProperties() {
    return new StardogProperties();
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
   * Elastic query properties.
   *
   * @return the elastic query properties
   */
  @Bean
  @ConfigurationProperties(prefix = "nci.evs.elasticsearch.query", ignoreUnknownFields = false)
  ElasticQueryProperties elasticQueryProperties() {
    return new ElasticQueryProperties();
  }

  /**
   * Elastic server properties.
   *
   * @return the elastic server properties
   */
  @Bean
  @ConfigurationProperties(prefix = "nci.evs.elasticsearch.server", ignoreUnknownFields = false)
  ElasticServerProperties elasticServerProperties() {
    return new ElasticServerProperties();
  }

  /**
   * Thesaurus properties.
   *
   * @return the thesaurus properties
   */
  @Bean
  @ConfigurationProperties(prefix = "thesaurus.owl", ignoreUnknownFields = false)
  ThesaurusProperties thesaurusProperties() {
    return new ThesaurusProperties();
  }

  /**
   * Swagger properties.
   *
   * @return the swagger properties
   */
  @Bean
  @ConfigurationProperties(prefix = "swagger.documentation", ignoreUnknownFields = false)
  SwaggerProperties swaggerProperties() {
    return new SwaggerProperties();
  }

}
