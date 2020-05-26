package gov.nih.nci.evs.api.service;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.util.ConceptUtils;

/**
 * Reference implementation of {@link QueryBuilderService}. Includes hibernate
 * tags for MEME database.
 *
 */
@Service
@PropertySource("classpath:sparql-queries.properties")
public class QueryBuilderServiceImpl implements QueryBuilderService {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(QueryBuilderServiceImpl.class);

  /** The stardog properties. */
  @Autowired
  StardogProperties stardogProperties;

  /** The env. */
  @Autowired
  Environment env;

  /**
   * Contruct prefix.
   *
   * @param source the source
   * @return the string
   */
  @Override
  public String contructPrefix(String source) {
    String prefix = env.getProperty("prefix.common");

    if (StringUtils.isNotEmpty(source)) {
      Map<String, String> values = ConceptUtils.asMap("source", source);
      prefix = getResolvedProperty("prefix.graph", values) + System.getProperty("line.separator")
          + prefix;
    }

    log.debug("prefix - " + prefix);
    return prefix;
  }

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param namedGraph the named graph
   * @return the string
   */
  @Override
  public String constructQuery(String queryProp, String namedGraph) {
    Map<String, String> values = ConceptUtils.asMap("namedGraph", namedGraph);
    String query = getResolvedProperty(queryProp, values);
    log.debug("construct " + queryProp + " - " + query);
    return query;
  }

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  @Override
  public String constructQuery(String queryProp, String conceptCode, String namedGraph) {
    Map<String, String> values =
        ConceptUtils.asMap("conceptCode", conceptCode, "namedGraph", namedGraph);
    String query = getResolvedProperty(queryProp, values);
    log.debug("construct " + queryProp + " - " + query);
    return query;
  }

  /**
   * Construct batch query.
   *
   * @param queryProp the query prop
   * @param namedGraph the named graph
   * @param inClause the in clause
   * @return the string
   */  
  @Override
  public String constructBatchQuery(String queryProp, String namedGraph, String inClause) {
    Map<String, String> values =
        ConceptUtils.asMap("namedGraph", namedGraph, "inClause", inClause);
    String query = getResolvedProperty(queryProp, values);
    log.debug("construct " + queryProp + " - " + query);
    return query;
  }
  
  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param values the values
   * @return the string
   */
  @Override
  public String constructQuery(String queryProp, Map<String, String> values) {
    String query = getResolvedProperty(queryProp, values);
    log.debug("construct " + queryProp + " - " + query);
    return query;
  }

  /**
   * Returns the resolved property.
   *
   * @param queryKey the query key
   * @param values the values
   * @return the resolved property
   */
  private String getResolvedProperty(String queryKey, Map<String, String> values) {
    return StringSubstitutor.replace(env.getProperty(queryKey), values, "#{", "}");
  }

}