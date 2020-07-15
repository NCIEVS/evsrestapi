
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
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
  @Override
  public void bulkIndex(List<Object> objects, String index, String type, Class clazz)
    throws IOException {
    if (CollectionUtils.isEmpty(objects))
      return;
    List<IndexQuery> indexQueries = new ArrayList<>();

    for (Object obj : objects) {
      indexQueries.add(new IndexQueryBuilder().withObject(clazz.cast(obj)).withIndexName(index)
          .withType(type).build());
    }

    operations.bulkIndex(indexQueries);
  }

  /* see superclass */
  public void loadMetric(Metric metric, String index) throws IOException {
    if (metric == null)
      return;

    final IndexQuery query =
        new IndexQueryBuilder().withObject(metric).withIndexName(index).withType("_doc").build();
    operations.index(query);
  }

  /* see superclass */
  @Override
  public void index(Object object, String index, String type, Class clazz) throws IOException {
    IndexQuery query = new IndexQueryBuilder().withObject(clazz.cast(object)).withIndexName(index)
        .withType(type).build();

    operations.index(query);
  }

  /* see superclass */
  public ElasticsearchOperations getElasticsearchOperations() {
    return operations;
  }
}
