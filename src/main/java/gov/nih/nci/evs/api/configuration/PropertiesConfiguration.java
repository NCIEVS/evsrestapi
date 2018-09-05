package gov.nih.nci.evs.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.properties.ElasticQueryProperties;
import gov.nih.nci.evs.api.properties.ElasticServerProperties;
import gov.nih.nci.evs.api.properties.StardogProperties;



@Configuration
@EnableConfigurationProperties
public class PropertiesConfiguration {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(PropertiesConfiguration.class);

    public PropertiesConfiguration() {
        log.debug("Creating instance of class PropertiesConfiguration");
    }


    /*
     * Server Properties
     */
    @Bean
    ServerProperties serverProperties() {
        return new ServerProperties();
    }
    
    /*
     * Stardog  Properties
     */
    @Bean
    @ConfigurationProperties(prefix = "nci.evs.stardog", ignoreUnknownFields = false)
    StardogProperties stardogProperties() {
        return new StardogProperties();
    }
    
    /*
     * Application  Properties
     */
    @Bean
    @ConfigurationProperties(prefix = "nci.evs.application", ignoreUnknownFields = false)
    ApplicationProperties applicationProperties() {
        return new ApplicationProperties();
    }
    
    
    /*
     * Elastic  Query Properties
     */
    @Bean
    @ConfigurationProperties(prefix = "nci.evs.elasticsearch.query", ignoreUnknownFields = false)
    ElasticQueryProperties elasticQueryProperties() {
        return new ElasticQueryProperties();
    }

    
    /*
     * Elastic  Query Properties
     */
    @Bean
    @ConfigurationProperties(prefix = "nci.evs.elasticsearch.server", ignoreUnknownFields = false)
    ElasticServerProperties elasticServerProperties() {
        return new ElasticServerProperties();
    }
   

}
