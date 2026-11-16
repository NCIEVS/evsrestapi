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
   * @param ignoreSources the ignore sources
   * @return the string
   */
  public String constructGraphQuery(String queryProp, List<String> ignoreSources);

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param terminology the terminology
   * @return the string
   */
  public String constructQuery(String queryProp, Terminology terminology);

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param terminology the terminology
   * @param conceptCode the concept code
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
   * @param terminology the terminology
   * @return the string
   */
  public String constructPrefix(Terminology terminology);

  /**
   * Prep sparql.
   *
   * @param terminology the terminology
   * @param query the query
   * @return the string
   */
  public String prepSparql(final Terminology terminology, final String query);

  /**
   * Prep sparql.
   *
   * @param terminology the terminology
   * @param query the query
   * @param keepPrefixes the prefixes
   * @return the string
   */
  public String prepSparql(
      final Terminology terminology, final String query, final Boolean keepPrefixes);
}
