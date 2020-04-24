package gov.nih.nci.evs.api.configuration;

import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;

@Configuration
public class ElasticsearchConfiguration {
  
  private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfiguration.class);
  
  @Autowired
  Environment env;
  
  @Bean
  RestHighLevelClient client() {
    String esHost = env.getProperty("nci.evs.elasticsearch.server.host") + ":" + env.getProperty("nci.evs.elasticsearch.server.port");
    logger.info(String.format("Configuring es client for host %s", esHost));
    ClientConfiguration clientConfiguration = ClientConfiguration.builder() 
      .connectedTo(esHost)
      .build();

    return RestClients.create(clientConfiguration).rest();                  
  }
}
