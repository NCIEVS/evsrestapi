package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Metric;
import gov.nih.nci.evs.api.support.es.IndexMetadata;
import java.io.IOException;
import java.util.List;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

// TODO: Auto-generated Javadoc
/**
 * The service for performing index related operations on Elasticsearch.
 *
 * @author Arun
 */
public interface ElasticOperationsService {

  /** The index in ES for index metadata *. */
  public static final String METADATA_INDEX = "evs_metadata";

  /** The index in ES for index mappings *. */
  public static final String MAPPINGS_INDEX = "evs_mappings";

  /** The index in ES for index mapsets *. */
  public static final String MAPSET_INDEX = "evs_mapsets";

  /**
   * create index using the given index name.
   *
   * @param indexName the index name
   * @param force whether index is to be deleted, if already present
   * @return if the index was created
   * @throws IOException the io exception
   */
  boolean createIndex(String indexName, boolean force) throws IOException;

  /**
   * load objects.
   *
   * @param objects the list of objects
   * @param index the index name
   * @param clazz the clazz
   * @throws IOException the io exception
   */
  @SuppressWarnings("rawtypes")
  void bulkIndex(List objects, String index, Class clazz) throws IOException;

  /**
   * Bulk index and wait.
   *
   * @param objects the objects
   * @param index the index
   * @param clazz the clazz
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("rawtypes")
  void bulkIndexAndWait(List objects, String index, Class clazz) throws IOException;

  /**
   * load object.
   *
   * @param object the object
   * @param index the index name
   * @param clazz the clazz
   * @throws IOException the io exception
   */
  void index(Object object, String index, @SuppressWarnings("rawtypes") Class clazz)
      throws IOException;

  /**
   * load metrics.
   *
   * @param metric the metric to load
   * @param index the index name
   * @throws IOException the io exception
   */
  void loadMetric(Metric metric, String index) throws IOException;

  /**
   * get the instance of {@code ElasticsearchOperations}.
   *
   * @return the instance of {@code ElasticsearchOperations}
   */
  ElasticsearchOperations getElasticsearchOperations();

  /**
   * Delete index.
   *
   * @param index the index
   * @return true, if successful
   */
  boolean deleteIndex(String index);

  /**
   * Delete.
   *
   * @param indexName the index name
   * @param id the id
   */
  String delete(String indexName, String id);

  /**
   * Delete the {@link IndexMetadata} object.
   *
   * @param id the id of the {@link IndexMetadata} object
   */
  String deleteIndexMetadata(String id);

  /**
   * Delete the query from the index.
   *
   * @param query the query to delete
   * @param indexName the name of the index
   * @return success or failure to delete
   */
  Boolean deleteQuery(String query, String indexName);
}
