package gov.nih.nci.evs.api.configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ElasticConfiguration {

  /** the logger **/
  private static final Logger logger = LoggerFactory.getLogger(ElasticConfiguration.class);

  /** the environment with properties **/
  @Autowired
  Environment env;

  @Bean
  Client client() throws UnknownHostException {
    if (logger.isDebugEnabled()) {
      logger.debug("Configuring elasticsearch client");
      logger.debug("    cluster name : {}", env.getProperty("nci.evs.elasticsearch.server.clusterName"));
      logger.debug("    server host  : {}", env.getProperty("nci.evs.elasticsearch.server.host"));
      logger.debug("    server port  : {}", env.getProperty("nci.evs.elasticsearch.server.port"));
    }
    
    Settings settings = Settings.builder()
        .put("cluster.name", env.getProperty("nci.evs.elasticsearch.server.clusterName")).build();
    TransportClient client = new PreBuiltTransportClient(settings);
    client.addTransportAddress(
        new TransportAddress(InetAddress.getByName(env.getProperty("nci.evs.elasticsearch.server.host")),
            Integer.parseInt(env.getProperty("nci.evs.elasticsearch.server.port"))));
    return client;
  }
}
