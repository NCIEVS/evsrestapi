package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import gov.nih.nci.evs.api.model.Concept;

/**
 * The service for performing index related operations on Elasticsearch
 * 
 * @author Arun
 *
 */
public interface ElasticOperationsService {
  /** The index name in ES for concepts **/
  public static final String CONCEPT_INDEX = "concept-test";
  
  /** The type in ES for concepts **/
  public static final String CONCEPT_TYPE = "concept-test";
  
  boolean createIndex(String indexName, boolean force) throws IOException;
  void loadConcepts(List<Concept> concepts, String index, String type) throws IOException;
  ElasticsearchOperations getElasticsearchOperations();
}
