
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.evs.api.model.Metric;

/**
 * The implementation for {@link ElasticOperationsService}.
 *
 * @author Arun
 */
@Service
public class ElasticOperationsServiceImpl implements ElasticOperationsService {

  /** The Constant logger. */
  private static final Logger logger = LoggerFactory.getLogger(ElasticOperationsServiceImpl.class);

  /** Elasticsearch operations *. */
  @Autowired
  ElasticsearchOperations operations;

  /* see superclass */
  @Override
  public boolean createIndex(String index, boolean force) throws IOException {
    boolean indexExists = operations.indexExists(index);

    if (indexExists) {
      if (!force) {
        logger.warn("Index {} already exists. Skipping index creation", index);
        return false;
      }
      operations.deleteIndex(index);
    }

    // create index
    operations.createIndex(index);
    return true;
  }

  /* see superclass */
  @SuppressWarnings("rawtypes")
  @Override
  public void bulkIndex(List objects, String index, String type, Class clazz) throws IOException {
    if (CollectionUtils.isEmpty(objects))
      return;
    List<IndexQuery> indexQueries = new ArrayList<>();

    for (Object obj : objects) {
      indexQueries.add(new IndexQueryBuilder().withObject(clazz.cast(obj)).withIndexName(index)
          .withType(type).build());
    }

    // operations.bulkIndex(indexQueries,
    // BulkOptions.builder().withRefreshPolicy(RefreshPolicy.WAIT_UNTIL).build());
    operations.bulkIndex(indexQueries);
  }

  /* see superclass */
  @SuppressWarnings("rawtypes")
  @Override
  public void bulkIndexAndWait(List objects, String index, String type, Class clazz)
    throws IOException {
    if (CollectionUtils.isEmpty(objects))
      return;
    List<IndexQuery> indexQueries = new ArrayList<>();

    for (Object obj : objects) {
      indexQueries.add(new IndexQueryBuilder().withObject(clazz.cast(obj)).withIndexName(index)
          .withType(type).build());
    }

    operations.bulkIndex(indexQueries,
        BulkOptions.builder().withRefreshPolicy(RefreshPolicy.WAIT_UNTIL).build());

  }

  /* see superclass */
  public void loadMetric(Metric metric, String index) throws IOException {
    if (metric == null)
      return;

    final IndexQuery query = new IndexQueryBuilder().withObject(metric).withIndexName(index)
        .withType(ElasticOperationsService.METRIC_TYPE).build();
    // BAC: removed this, we do not need to put the mapping on each request
    // operations.putMapping(index, ElasticOperationsService.METRIC_TYPE,
    // Metric.class);
    try {
      operations.index(query);
    } catch (Exception e) {
      // This happens on monthly switch-over and we need to create a new index
      if (e.getMessage().contains("index_not_found_exception")) {
        boolean result = operations.createIndex(index, false);
        if (result) {
          operations.putMapping(index, ElasticOperationsService.METRIC_TYPE, Metric.class);
        }
      }
    }
  }

  /* see superclass */
  @Override
  public void index(Object object, String index, String type,
    @SuppressWarnings("rawtypes") Class clazz) throws IOException {
    IndexQuery query = new IndexQueryBuilder().withObject(clazz.cast(object)).withIndexName(index)
        .withType(type).build();

    operations.index(query);
  }

  /* see superclass */
  @Override
  public boolean deleteIndex(String index) {
    return operations.deleteIndex(index);
  }

  /* see superclass */
  public ElasticsearchOperations getElasticsearchOperations() {
    return operations;
  }

  @Override
  public void delete(String indexName, String type, String id) {
    operations.delete(indexName, type, id);
  }
}
