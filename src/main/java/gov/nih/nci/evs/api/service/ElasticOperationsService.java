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
  
  /**
   * create index using the given index name
   * 
   * @param indexName the index name
   * @param force whether index is to be deleted, if already present
   * @return if the index was created
   * @throws IOException the io exception
   */
  boolean createIndex(String indexName, boolean force) throws IOException;
  
  /**
   * load concepts
   * 
   * @param concepts the list of concepts
   * @param index the index name
   * @param type the type name
   * @throws IOException the io exception
   */
  void loadConcepts(List<Concept> concepts, String index, String type) throws IOException;
  
  /**
   * get the instance of {@code ElasticsearchOperations}
   * 
   * @return the instance of {@code ElasticsearchOperations}
   */
  ElasticsearchOperations getElasticsearchOperations();
}
