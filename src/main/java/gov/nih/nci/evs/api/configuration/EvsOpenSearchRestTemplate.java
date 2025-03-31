package gov.nih.nci.evs.api.configuration;

import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * Custom {@link OpenSearchRestTemplate} to log queries.
 *
 * <p>As of spring-data-elasticsearch version 3.2.0, only one query method logs query and honors
 * spring data logging property. This custom implementation is to cover for that gap.
 *
 * @author Arun
 */
public class EvsOpenSearchRestTemplate extends OpenSearchRestTemplate {

  /** The Constant logger. */
  private static final Logger logger = LoggerFactory.getLogger(EvsOpenSearchRestTemplate.class);

  /**
   * Instantiates a {@link EvsOpenSearchRestTemplate} from the specified parameters.
   *
   * @param client the client
   */
  public EvsOpenSearchRestTemplate(final RestHighLevelClient client) {
    super(client);
  }

  /* see superclass */
  @Override
  public <T> SearchHits<T> search(
      final Query query, final Class<T> clazz, final IndexCoordinates index) {
    if (logger.isDebugEnabled() && ((NativeSearchQuery) query).getQuery() != null) {
      logger.debug("  opensearch query = \n" + ((NativeSearchQuery) query).getQuery());
    }

    return super.search(query, clazz, index);
  }
}
