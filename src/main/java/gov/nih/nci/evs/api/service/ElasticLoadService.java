package gov.nih.nci.evs.api.service;

import java.io.IOException;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;

/**
 * The service to load concepts to Elasticsearch
 * 
 * Retrieves concepts from stardog and creates necessary index on Elasticsearch
 * 
 * @author Arun
 *
 */
public interface ElasticLoadService {
  /**
   * Load cached objects to elasticsearch.
   * 
   * @param config the load config from command line input
   * @param terminology the terminology
   * @throws IOException the io exception
   */
  void loadObjects(ElasticLoadConfig config, Terminology terminology) throws IOException;
  
  /**
   * Load concepts to elasticsearch.
   * 
   * @param config the load config from command line input
   * @param terminology the terminology
   * @throws IOException the io exception
   */
  void loadConcepts(ElasticLoadConfig config, Terminology terminology) throws IOException;
}
