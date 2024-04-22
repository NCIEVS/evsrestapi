package gov.nih.nci.evs.api.configuration;

import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * Custom {@link ElasticsearchRestTemplate} to log queries.
 *
 * <p>As of spring-data-elasticsearch version 3.2.0, only one query method logs query and honors
 * spring data logging property. This custom implementation is to cover for that gap.
 *
 * @author Arun
 */
public class EVSElasticsearchRestTemplate extends ElasticsearchRestTemplate {

  /** The Constant logger. */
  private static final Logger logger = LoggerFactory.getLogger(EVSElasticsearchRestTemplate.class);

  /**
   * Instantiates a {@link EVSElasticsearchRestTemplate} from the specified parameters.
   *
   * @param client the client
   */
  public EVSElasticsearchRestTemplate(RestHighLevelClient client) {
    super(client);
  }

  /* see superclass */
  public <T> SearchHits<T> search(Query query, Class<T> clazz, IndexCoordinates index) {

    if (logger.isDebugEnabled() && ((NativeSearchQuery) query).getQuery() != null) {
      logger.debug("  elasticsearch query = \n" + ((NativeSearchQuery) query).getQuery());
    }

    return super.search(query, clazz, index);
  }
}
