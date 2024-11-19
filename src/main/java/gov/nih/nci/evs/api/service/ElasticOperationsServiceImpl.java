package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.Metric;
import gov.nih.nci.evs.api.support.es.IndexMetadata;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
  @Autowired ElasticsearchOperations operations;

  /* see superclass */
  @Override
  public boolean createIndex(String index, boolean force) throws IOException {
    boolean indexExists = operations.indexOps(IndexCoordinates.of(index)).exists();

    if (indexExists) {
      if (!force) {
        logger.warn("Index {} already exists. Skipping index creation", index);
        return false;
      }
      operations.indexOps(IndexCoordinates.of(index)).delete();
    }

    // create index
    operations.indexOps(IndexCoordinates.of(index)).create();
    return true;
  }

  /* see superclass */
  @SuppressWarnings("rawtypes")
  @Override
  public void bulkIndex(List objects, String index, Class clazz) throws IOException {
    if (CollectionUtils.isEmpty(objects)) return;
    List<IndexQuery> indexQueries = new ArrayList<>();

    for (Object obj : objects) {
      indexQueries.add(new IndexQueryBuilder().withObject(clazz.cast(obj)).build());
    }

    // operations.bulkIndex(indexQueries,
    // BulkOptions.builder().withRefreshPolicy(RefreshPolicy.WAIT_UNTIL).build());
    operations.bulkIndex(indexQueries, IndexCoordinates.of(index));
  }

  /* see superclass */
  @SuppressWarnings("rawtypes")
  @Override
  public void bulkIndexAndWait(List objects, String index, Class clazz) throws IOException {
    if (CollectionUtils.isEmpty(objects)) return;
    List<IndexQuery> indexQueries = new ArrayList<>();

    for (Object obj : objects) {
      indexQueries.add(new IndexQueryBuilder().withObject(clazz.cast(obj)).build());
    }

    operations.bulkIndex(
        indexQueries,
        BulkOptions.builder().withRefreshPolicy(RefreshPolicy.WAIT_UNTIL).build(),
        IndexCoordinates.of(index));
  }

  /* see superclass */
  @Override
  public void loadMetric(Metric metric, String index) throws IOException {
    if (metric == null) return;

    final IndexQuery query = new IndexQueryBuilder().withObject(metric).build();
    // BAC: removed this, we do not need to put the mapping on each request
    // operations.putMapping(index, ElasticOperationsService.METRIC_TYPE,
    // Metric.class);
    try {
      operations.index(query, IndexCoordinates.of(index));
    } catch (Exception e) {
      // This happens on monthly switch-over and we need to create a new index
      if (e.getMessage().contains("index_not_found_exception")) {
        boolean result = operations.indexOps(IndexCoordinates.of(index)).create();
        if (result) {
          operations.indexOps(IndexCoordinates.of(index)).putMapping(Metric.class);
        }
      }
    }
  }

  /* see superclass */
  @Override
  public void index(Object object, String index, @SuppressWarnings("rawtypes") Class clazz)
      throws IOException {
    IndexQuery query = new IndexQueryBuilder().withObject(clazz.cast(object)).build();

    operations.index(query, IndexCoordinates.of(index));
  }

  /* see superclass */
  @Override
  public boolean deleteIndex(String index) {
    return operations.indexOps(IndexCoordinates.of(index)).delete();
  }

  /* see superclass */
  @Override
  public ElasticsearchOperations getElasticsearchOperations() {
    return operations;
  }

  @Override
  public String delete(String indexName, String id) {
    return operations.delete(id, IndexCoordinates.of(indexName));
  }

  /**
   * see superclass *.
   *
   * @param id the id of the {@link IndexMetadata} object
   */
  @Override
  public String deleteIndexMetadata(String id) {
    return operations.delete(id, IndexCoordinates.of(METADATA_INDEX));
  }

  /** */
  @Override
  public Boolean deleteQuery(String query, String indexName) {
    try {
      NativeSearchQuery deleteQuery =
          new NativeSearchQueryBuilder().withQuery(QueryBuilders.queryStringQuery(query)).build();
      operations.delete(deleteQuery, Mapping.class, IndexCoordinates.of(indexName));
      return true;
    } catch (Exception e) {
      logger.error("query delete failed: " + e.getMessage());
      return false;
    }
  }
}
