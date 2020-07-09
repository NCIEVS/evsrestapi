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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Metric;

/**
 * The implementation for {@link ElasticOperationsService}
 * 
 * @author Arun
 *
 */
@Service
public class ElasticOperationsServiceImpl implements ElasticOperationsService {

  private static final Logger logger = LoggerFactory.getLogger(ElasticOperationsServiceImpl.class);
  
  /** Elasticsearch operations **/
  @Autowired
  ElasticsearchOperations operations;
  
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
    
    //create index
    operations.createIndex(index);
    return true;
  }

  @Override
  public void loadConcepts(List<Concept> concepts, String index, String type) throws IOException {
    if (CollectionUtils.isEmpty(concepts)) return;
    
    List<IndexQuery> indexQueries = new ArrayList<>();
    
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    
    for(Concept concept: concepts) {
      indexQueries.add(new IndexQueryBuilder().withId(concept.getCode()).withObject(concept)
        .withIndexName(index).withType(type)
        .build());
    }
    
    operations.bulkIndex(indexQueries);
  }

  public void loadMetric(Metric metric, String index) throws IOException {
    if(metric == null) return;

    final IndexQuery query = new IndexQueryBuilder().withObject(metric).withIndexName(index).withType("_doc").build();
    operations.index(query);
  }
  
  public ElasticsearchOperations getElasticsearchOperations() {
    return operations;
  }
}
