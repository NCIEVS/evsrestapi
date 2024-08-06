package gov.nih.nci.evs.api.configuration;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

/** The Class ElasticConfiguration. */
@Configuration
public class ElasticConfiguration {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(ElasticConfiguration.class);

  /** the environment with properties *. */
  @Autowired Environment env;

  /**
   * Client.
   *
   * @return the rest high level client
   */
  @Bean
  RestHighLevelClient client() {

    // https://stackoverflow.com/questions/77856007/elasticsearch-resthighlevelclient-save-index-throws-nullpointerexception-in-java
    Header[] headers = {
      new BasicHeader("Accept", "application/vnd.elasticsearch+json;compatible-with=7"),
      new BasicHeader("Content-type", "application/vnd.elasticsearch+json;compatible-with=7")
    };

    final String esHost = env.getProperty("nci.evs.elasticsearch.server.host");
    final int esPort = Integer.parseInt(env.getProperty("nci.evs.elasticsearch.server.port"));
    final String esScheme = env.getProperty("nci.evs.elasticsearch.server.scheme");
    final int timeout = Integer.parseInt(env.getProperty("nci.evs.elasticsearch.timeout"));
    logger.info(String.format("Configuring es client for host %s %s %s", esHost, esPort, timeout));
    return new RestHighLevelClient(
        RestClient.builder(new HttpHost(esHost, esPort, esScheme))
            .setDefaultHeaders(headers)
            .setRequestConfigCallback(
                builder -> builder.setConnectTimeout(timeout).setSocketTimeout(timeout)));

    // ClientConfiguration clientConfiguration =
    // ClientConfiguration.builder().connectedTo(esHost)..build();
    // return RestClients.create(clientConfiguration).rest();
  }

  /**
   * Elastic rest template.
   *
   * @return the elasticsearch rest template
   */
  @SuppressWarnings("resource")
  @Bean(name = "elasticsearchTemplate")
  ElasticsearchRestTemplate elasticRestTemplate() {
    return new EVSElasticsearchRestTemplate(client());
  }
}
