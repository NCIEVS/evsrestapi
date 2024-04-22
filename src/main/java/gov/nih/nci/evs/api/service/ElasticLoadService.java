package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import java.io.IOException;
import org.springframework.context.ApplicationContext;

/**
 * The service to load concepts to Elasticsearch
 *
 * <p>Retrieves concepts from stardog and creates necessary index on Elasticsearch
 *
 * @author Arun
 */
public interface ElasticLoadService {

  /**
   * Initialize the service to ensure indexes are created.
   *
   * @throws Exception
   */
  public void initialize() throws IOException;

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
   * @return number of concepts loaded
   * @throws IOException the io exception
   * @throws Exception
   */
  int loadConcepts(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
      throws IOException, Exception;

  /**
   * Clean stale indexes.
   *
   * @param terminology the terminology
   * @throws Exception the exception
   */
  void cleanStaleIndexes(Terminology terminology) throws Exception;

  /**
   * Update latest flag.
   *
   * @param terminology the terminology
   * @throws Exception the exception
   */
  void updateLatestFlag(Terminology terminology) throws Exception;

  /**
   * Get Terminology object
   *
   * @param app the application context object
   * @param config the config object
   * @param filepath the filepath
   * @param termName the terminology name
   * @param forceDelete force delete terminology
   * @return Terminology
   * @throws Exception the exception
   */
  Terminology getTerminology(
      ApplicationContext app,
      ElasticLoadConfig config,
      String filepath,
      String termName,
      boolean forceDelete)
      throws Exception;

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
   * get hierarchy utils.
   *
   * @param term the terminology object
   * @return HierarchyUtils
   * @throws Exception the exception
   */
  HierarchyUtils getHierarchyUtils(Terminology term) throws Exception;
}
