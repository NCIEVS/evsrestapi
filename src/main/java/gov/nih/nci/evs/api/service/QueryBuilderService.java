
package gov.nih.nci.evs.api.service;

import java.util.List;
import java.util.Map;

/**
 * Query builder service.
 */
public interface QueryBuilderService {

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructQuery(String queryProp, String codeCode, String namedGraph);

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructQuery(String queryProp, String codeCode, String conceptCode, String namedGraph);

  /**
   * Construct batch query.
   *
   * @param queryProp the query prop
   * @param namedGraph the named graph
   * @param conceptCodes the concept codes
   * @return the string
   */
  public String constructBatchQuery(String queryProp, String namedGraph, List<String> conceptCodes);

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param paramMap the param map
   * @return the string
   */
  public String constructQuery(String queryProp, Map<String, String> paramMap);

  /**
   * Contruct prefix.
   *
   * @param source the graph source
   * @return the string
   */
  public String contructPrefix(String source);

}
