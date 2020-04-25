package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
  
  /** The index name in ES for concepts **/
  public static final String CONCEPT_INDEX = "concept";
  
  /** The type in ES for concepts **/
  public static final String CONCEPT_TYPE = "concept";
  
  /** Elasticsearch client **/
  @Autowired
  RestHighLevelClient esClient;
  
  @Override
  public void createIndex(String index, boolean force) throws IOException {
    boolean indexExists = esClient.indices().exists(new GetIndexRequest(index), RequestOptions.DEFAULT);
    
    if (indexExists) {
      if (!force) {
        logger.warn("Index {} already exists. Skipping index creation", index);
        return;
      }
      esClient.indices().delete(new DeleteIndexRequest(index), RequestOptions.DEFAULT);
    }
    
    //create index
    esClient.indices().create(new CreateIndexRequest(index), RequestOptions.DEFAULT);
  }

  @Override
  public void loadConcepts(List<Concept> concepts, String index, String type, boolean async) throws IOException {
    BulkRequest bulkRequest = new BulkRequest(index, type);
    
    for(Concept concept: concepts) {
      bulkRequest.add(new IndexRequest().id(concept.getCode()).source(concept));
    }
    
    if (async) {
      esClient.bulkAsync(bulkRequest, RequestOptions.DEFAULT, new ActionListener<BulkResponse>() {
        @Override
        public void onResponse(BulkResponse response) {
          // TODO Auto-generated method stub
          logger.info("Bulk load request successul!");
        }
        
        @Override
        public void onFailure(Exception e) {
          // TODO Auto-generated method stub
          logger.warn("Bulk load request failed with message - {}", e.getMessage());
        }
      });
    } else {
      esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }
    
  }
  
}
