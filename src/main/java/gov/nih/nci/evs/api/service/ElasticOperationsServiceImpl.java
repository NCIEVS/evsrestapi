package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;

/**
 * The implementation for {@link ElasticOperationsService}
 * 
 * @author Arun
 *
 */
@Service
public class ElasticOperationsServiceImpl implements ElasticOperationsService {

  private static final Logger logger = LoggerFactory.getLogger(ElasticOperationsServiceImpl.class);
  
  private static final TimeValue DEFAULT_BULK_TIMEOUT = TimeValue.timeValueMinutes(10);
  
  /** Elasticsearch operations **/
  @Autowired
  ElasticsearchOperations operations;
  
  @Override
  public void createIndex(String index, boolean force) throws IOException {
    boolean indexExists = operations.indexExists(index);
    
    if (indexExists) {
      if (!force) {
        logger.warn("Index {} already exists. Skipping index creation", index);
        return;
      }
      operations.deleteIndex(index);
    }
    
    //create index
    operations.createIndex(index);
  }

  @Override
  public void loadConcepts(List<Concept> concepts, String index, String type, TimeValue timeout) throws IOException {
    if (CollectionUtils.isEmpty(concepts)) return;
    BulkRequest bulkRequest = new BulkRequest(index, type);
    bulkRequest.timeout();
    
    List<IndexQuery> indexQueries = new ArrayList<>();
    
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    
    for(Concept concept: concepts) {
      indexQueries.add(new IndexQueryBuilder().withId(concept.getCode()).withObject(concept)
        .withIndexName(index).withType(type)
        .build());
    }
    
    BulkOptions options = BulkOptions.builder()
        .withTimeout(timeout == null ? DEFAULT_BULK_TIMEOUT : timeout)
        .build();
    
    operations.bulkIndex(indexQueries, options);
  }
  
}
