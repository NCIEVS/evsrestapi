package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Metric;
import gov.nih.nci.evs.api.support.es.ElasticObject;

/**
 * The service for performing index related operations on Elasticsearch
 * 
 * @author Arun
 *
 */
public interface ElasticOperationsService {
  
  /** The type in ES for concepts **/
  public static final String CONCEPT_TYPE = "concept";

  /** The type in ES for cached objects **/
  public static final String OBJECT_TYPE = "object";
  
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
   * load objects
   * 
   * @param objects the list of objects
   * @param index the index name
   * @param type the type name
   * @throws IOException the io exception
   */
  void bulkIndex(List<Object> objects, String index, String type, Class clazz) throws IOException;
  
  /**
   * load object
   * 
   * @param object the object
   * @param index the index name
   * @param type the type name
   * @throws IOException the io exception
   */
  void index(Object object, String index, String type, Class clazz) throws IOException;
  
  /**
   * load metrics
   * 
   * @param metric the metric to load
   * @param index the index name
   * @throws IOException the io exception
   */
  void loadMetric(Metric metric, String index) throws IOException;

  /**
   * get the instance of {@code ElasticsearchOperations}
   * 
   * @return the instance of {@code ElasticsearchOperations}
   */
  ElasticsearchOperations getElasticsearchOperations();

}
