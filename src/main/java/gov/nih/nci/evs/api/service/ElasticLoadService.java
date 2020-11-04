package gov.nih.nci.evs.api.service;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;

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
   * @param hierarchy the terminology hierarchy
   * @throws IOException the io exception
   * @throws Exception
   */
  void loadObjects(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
    throws IOException, Exception;

  /**
   * Load concepts to elasticsearch.
   * 
   * @param config the load config from command line input
   * @param terminology the terminology
   * @param hierarchy the hierarchy object
   * @param cmd command line objext
   * @return number of concepts loaded
   * @throws IOException the io exception
   * @throws Exception
   */
  int loadConcepts(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy,
    CommandLine cmd) throws IOException, Exception;

  /**
   * Clean stale indexes.
   *
   * @throws Exception the exception
   */
  void cleanStaleIndexes() throws Exception;

  /**
   * Update latest flag.
   * 
   * @param term the terminology object
   *
   * @throws Exception the exception
   */
  void updateLatestFlag(Terminology term) throws Exception;

  /**
   * Get Terminology object
   * 
   * @param app the application context object
   * @param config the config object
   * @param filepath the filepath
   * @param termName the terminology name
   * @return Terminology
   * 
   *
   * @throws Exception the exception
   */
  Terminology getTerminology(ApplicationContext app, ElasticLoadConfig config, String filepath,
    String termName) throws Exception;

  /**
   * check load status
   * 
   * @param totalConcepts the total number of concepts
   * @param term the terminology object
   * @throws IOException
   */
  void checkLoadStatus(int totalConcepts, Terminology term) throws IOException;

  /**
   * load index metadata
   * 
   * @param totalConcepts the total number of concepts
   * @param term the terminology object
   * @throws IOException
   */
  void loadIndexMetadata(int totalConcepts, Terminology term) throws IOException;

  /**
   * get hierarchy utils
   * 
   * @param term the terminology object
   * @return HierarchyUtils
   * @throws IOException
   * @throws JsonMappingException
   * @throws JsonParseException
   */
  HierarchyUtils getHierarchyUtils(Terminology term)
    throws JsonParseException, JsonMappingException, IOException;

}
