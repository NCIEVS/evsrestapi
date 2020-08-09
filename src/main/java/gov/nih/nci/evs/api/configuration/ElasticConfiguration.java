package gov.nih.nci.evs.api.configuration;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

@Configuration
public class ElasticConfiguration {

  /** the logger **/
  private static final Logger logger = LoggerFactory.getLogger(ElasticConfiguration.class);

  /** the environment with properties **/
  @Autowired
  Environment env;

  @Bean
  RestHighLevelClient client() {
    String esHost = env.getProperty("nci.evs.elasticsearch.server.host");
    int esPort = Integer.parseInt(env.getProperty("nci.evs.elasticsearch.server.port"));
    String esScheme = env.getProperty("nci.evs.elasticsearch.server.scheme");
    logger.info(String.format("Configuring es client for host %s", esHost));
    return new RestHighLevelClient(RestClient.builder(new HttpHost(esHost, esPort, esScheme)));
    // ClientConfiguration clientConfiguration =
    // ClientConfiguration.builder().connectedTo(esHost)..build();
    // return RestClients.create(clientConfiguration).rest();
  }
  
  @Bean(name = "elasticsearchTemplate")
  ElasticsearchRestTemplate elasticRestTemplate() {
    return new EVSElasticsearchRestTemplate(client());
  }
}
