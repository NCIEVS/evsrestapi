package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import java.util.Map;

/** Query builder service. */
public interface QueryBuilderService {

  /**
   * Construct graph query.
   *
   * @param queryProp the query prop
   * @return the string
   */
  public String constructGraphQuery(String queryProp, List<String> ignoreSources);

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructQuery(String queryProp, Terminology terminology);

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructQuery(String queryProp, Terminology terminology, String conceptCode);

  /**
   * Construct batch query.
   *
   * @param queryProp the query prop
   * @param terminology the terminology
   * @param conceptCodes the concept codes
   * @return the string
   */
  public String constructBatchQuery(
      String queryProp, Terminology terminology, List<String> conceptCodes);

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param terminology the terminology
   * @param paramMap the param map
   * @return the string
   */
  public String constructQuery(
      String queryProp, Terminology terminology, Map<String, String> paramMap);

  /**
   * Contruct prefix.
   *
   * @param source the graph source
   * @return the string
   */
  public String constructPrefix(Terminology terminology);
}
