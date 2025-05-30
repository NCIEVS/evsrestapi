package gov.nih.nci.evs.api.configuration;

// import static org.mockito.Mockito.timeout;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.core.OpenSearchOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** Configuration for opensearch. */
@Configuration
public class OpensearchConfiguration {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(OpensearchConfiguration.class);

  /** the environment with properties *. */
  @Autowired Environment env;

  /**
   * Client.
   *
   * @return the rest high level client
   */
  @Bean
  RestHighLevelClient client() {
    final String osHost = env.getProperty("nci.evs.opensearch.server.host");
    final int osPort = Integer.parseInt(env.getProperty("nci.evs.opensearch.server.port"));
    final String osScheme = env.getProperty("nci.evs.opensearch.server.scheme");
    final int timeout = Integer.parseInt(env.getProperty("nci.evs.opensearch.timeout"));
    logger.info(
        String.format("Configuring opensearch client for host %s %s %s", osHost, osPort, timeout));
    return new RestHighLevelClient(
        RestClient.builder(new HttpHost(osHost, osPort, osScheme))
            .setRequestConfigCallback(
                builder -> builder.setConnectTimeout(timeout).setSocketTimeout(timeout)));

    // Alternate:
    // ClientConfiguration clientConfiguration =
    // ClientConfiguration.builder().connectedTo(esHost)..build();
    // return RestClients.create(clientConfiguration).rest();
  }

  /**
   * Open search operations. This is needed in order to inject an OpenSearchOperations into the
   * right place.
   *
   * @return the open search operations
   */
  @SuppressWarnings("resource")
  @Bean
  public OpenSearchOperations openSearchOperations() {
    return new EvsOpenSearchRestTemplate(client());
  }
}
