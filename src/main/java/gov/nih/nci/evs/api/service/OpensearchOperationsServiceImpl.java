package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.Metric;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * The implementation for {@link OpensearchOperationsService}.
 *
 * @author Arun
 */
@Service
public class OpensearchOperationsServiceImpl implements OpensearchOperationsService {

  /** The Constant logger. */
  private static final Logger logger =
      LoggerFactory.getLogger(OpensearchOperationsServiceImpl.class);

  /** Opensearch operations *. */
  @Autowired OpenSearchOperations operations;

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

  /**
   * Returns the mapping.
   *
   * @param name the name
   * @return the mapping
   * @throws IOException Signals that an I/O exception has occurred.
   */
  /* see superclass */
  @Override
  public Map<String, Object> getMapping(String name) throws IOException {
    return operations.indexOps(IndexCoordinates.of(name)).getMapping();
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
    // operations.putMapping(index, OpensearchOperationsService.METRIC_TYPE,
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

  @Override
  public void update(
      String id, Object object, String index, @SuppressWarnings("rawtypes") Class clazz)
      throws IOException {
    // don't lose @WriteOnlyProperty on update
    if (object instanceof Concept concept) {
      concept.setSynonyms(null);
      concept.setDefinitions(null);
    }

    // don't let update add empty lists
    nullifyEmptyLists(object);
    Document doc = operations.getElasticsearchConverter().mapObject(object);

    UpdateQuery query = UpdateQuery.builder(id).withDocument(doc).build();

    operations.update(query, IndexCoordinates.of(index));
  }

  private void nullifyEmptyLists(Object obj) {
    for (Field field : obj.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      try {
        Object value = field.get(obj);
        if (value instanceof List && ((List<?>) value).isEmpty()) {
          field.set(obj, null);
        }
      } catch (IllegalAccessException ignored) {
        // n/a
      }
    }
  }

  /* see superclass */
  @Override
  public boolean deleteIndex(String index) {
    return operations.indexOps(IndexCoordinates.of(index)).delete();
  }

  /* see superclass */
  @Override
  public OpenSearchOperations getOpenSearchOperations() {
    return operations;
  }

  /* see superclass */
  @Override
  public String delete(String indexName, String id) {
    return operations.delete(id, IndexCoordinates.of(indexName));
  }

  /* see superclass */
  @Override
  public String deleteIndexMetadata(String id) {
    return operations.delete(id, IndexCoordinates.of(METADATA_INDEX));
  }

  /* see superclass */
  @Override
  public Boolean deleteQuery(String query, String indexName) throws Exception {
    final NativeSearchQuery deleteQuery =
        new NativeSearchQueryBuilder().withQuery(QueryBuilders.queryStringQuery(query)).build();

    operations.delete(
        DeleteQuery.builder(deleteQuery).build(), Mapping.class, IndexCoordinates.of(indexName));
    return true;
  }
}
