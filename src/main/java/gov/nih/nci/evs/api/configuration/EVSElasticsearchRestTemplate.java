package gov.nih.nci.evs.api.configuration;

import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

/**
 * Custom {@link ElasticsearchRestTemplate} to log queries.
 * 
 * As of spring-data-elasticsearch version 3.2.0, only one query method logs query and honors 
 * spring data logging property. This custom implementation is to cover for that gap.
 * 
 * @author Arun
 *
 */
public class EVSElasticsearchRestTemplate extends ElasticsearchRestTemplate {

  private static final Logger logger = LoggerFactory.getLogger(EVSElasticsearchRestTemplate.class);
  
  public EVSElasticsearchRestTemplate(RestHighLevelClient client) {
    super(client);
  }
  
  @Override
  public <T> AggregatedPage<T> queryForPage(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
    if (logger.isDebugEnabled()) {
      logger.debug("doSearch query:\n" + query.getQuery().toString());
    }    
    
    return super.queryForPage(query, clazz, mapper);
  }
  
}
