package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.OpensearchLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationContext;

/**
 * The service to load concepts to OpenSearch.
 *
 * <p>Retrieves concepts from graph db and creates necessary index on OpenSearch.
 *
 * @author Arun
 */
public interface OpensearchLoadService {

  /**
   * Initialize the service to ensure indexes are created.
   *
   * @throws Exception
   */
  public void initialize() throws IOException, Exception;

  /**
   * Load cached objects to Opensearch.
   *
   * @param config the load config from command line input
   * @param terminology the terminology
   * @param hierarchy the terminology hierarchy
   * @throws IOException the io exception
   * @throws Exception
   */
  void loadObjects(OpensearchLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
      throws IOException, Exception;

  /**
   * Load concepts to Opensearch.
   *
   * @param config the load config from command line input
   * @param terminology the terminology
   * @param hierarchy the hierarchy object
   * @param historyMap the history map
   * @return number of concepts loaded
   * @throws IOException the io exception
   * @throws Exception
   */
  int loadConcepts(
      OpensearchLoadConfig config,
      Terminology terminology,
      HierarchyUtils hierarchy,
      Map<String, List<Map<String, String>>> historyMap)
      throws IOException, Exception;

  /**
   * Clean stale indexes.
   *
   * @param terminology the terminology
   * @throws Exception the exception
   */
  Set<String> cleanStaleIndexes(Terminology terminology) throws Exception;

  /**
   * Update latest flag.
   *
   * @param terminology the terminology
   * @param removed the removed terminologyVersion combinations (by cleanStaleIndexes). Needed
   *     because Opensearch may not have yet finished removing them.
   * @throws Exception the exception
   */
  void updateLatestFlag(Terminology terminology, Set<String> removed) throws Exception;

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
      OpensearchLoadConfig config,
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
  void checkLoadStatus(int totalConcepts, Terminology term) throws IOException, Exception;

  /**
   * load index metadata
   *
   * @param totalConcepts the total number of concepts
   * @param term the terminology object
   * @throws IOException
   */
  void loadIndexMetadata(int totalConcepts, Terminology term) throws IOException, Exception;

  /**
   * get hierarchy utils.
   *
   * @param term the terminology object
   * @return HierarchyUtils
   * @throws Exception the exception
   */
  HierarchyUtils getHierarchyUtils(Terminology term) throws Exception;

  /**
   * Update history.
   *
   * @param terminology the terminology
   * @param historyMap the history map
   * @throws Exception the exception
   */
  void updateHistory(
      final Terminology terminology, Map<String, List<Map<String, String>>> historyMap)
      throws Exception;

  /**
   * Update history map.
   *
   * @param terminology the terminology
   * @param filepath the new history filepath
   * @throws Exception the exception
   */
  public Map<String, List<Map<String, String>>> updateHistoryMap(
      final Terminology terminology, final String filepath) throws Exception;
}
