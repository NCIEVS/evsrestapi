

package gov.nih.nci.evs.api.service;

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
  public String constructQuery(String queryProp, String namedGraph);

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructQuery(String queryProp, String conceptCode, String namedGraph);

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
