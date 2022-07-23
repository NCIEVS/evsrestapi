
package gov.nih.nci.evs.api.service;

import java.util.List;
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
  @SuppressWarnings("unused")
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
  public String contructPrefix(final String source) {
    String prefix = env.getProperty("prefix.common");

    if (StringUtils.isNotEmpty(source)) {
      Map<String, String> values = ConceptUtils.asMap("source", source);
      final String terminology = source.replaceFirst(".*\\/(.*)\\.owl", "$1").toLowerCase();

      // If we do not extract a terminology value, compose "graph" and base
      // prefixes
      if (terminology.equals(source)) {
        prefix = getResolvedProperty("prefix.graph", values) + System.getProperty("line.separator")
            + prefix;
      }
      // otherwise, if we can and there is a property for it, include those also
      else if (env.containsProperty("prefix." + terminology)) {
        prefix = getResolvedProperty("prefix.graph", values) + System.getProperty("line.separator")
            + getResolvedProperty("prefix." + terminology, values)
            + System.getProperty("line.separator") + prefix + " ";
      }
    }

    // log.debug("prefix - " + prefix);
    return prefix;
  }

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param codeCode the code code
   * @param namedGraph the named graph
   * @return the string
   */
  @Override
  public String constructQuery(final String queryProp, final String codeCode,
    final String namedGraph) {
    Map<String, String> values = ConceptUtils.asMap("codeCode", codeCode, "namedGraph", namedGraph);
    String query = getResolvedProperty(queryProp, values);
    // log.debug("construct " + queryProp + " - " + query);
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
  public String constructQuery(final String queryProp, final String codeCode,
    final String conceptCode, final String namedGraph) {
    checkCode(conceptCode);
    final Map<String, String> values = ConceptUtils.asMap("codeCode", codeCode, "conceptCode",
        conceptCode, "namedGraph", namedGraph);
    final String query = getResolvedProperty(queryProp, values);
    // log.debug("construct " + queryProp + " - " + query);
    return query;
  }

  /**
   * Construct batch query.
   *
   * @param queryProp the query prop
   * @param namedGraph the named graph
   * @param conceptCodes the concept codes
   * @return the string
   */
  @Override
  public String constructBatchQuery(final String queryProp, final String namedGraph,
    final List<String> conceptCodes) {
    final String inClause = getInClause(conceptCodes);
    final Map<String, String> values =
        ConceptUtils.asMap("namedGraph", namedGraph, "inClause", inClause);
    final String query = getResolvedProperty(queryProp, values);
    // log.debug("construct " + queryProp + " - " + query);
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
    // Validate codes
    for (final Map.Entry<String, String> entry : values.entrySet()) {
      if (entry.getKey().toLowerCase().contains("code")) {
        checkCode(entry.getValue());
      }
    }
    String query = getResolvedProperty(queryProp, values);
    // log.debug("construct " + queryProp + " - " + query);
    // log.debug("construct map " + values);
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

  /**
   * Returns the in clause.
   *
   * @param values the values
   * @return the in clause
   */
  private String getInClause(List<String> values) {
    checkCodes(values);
    return new StringBuilder().append("'").append(String.join("', '", values)).append("'")
        .toString();
  }

  /**
   * Check concept code.
   *
   * @param codes the codes
   */
  private void checkCodes(final List<String> codes) {
    for (final String code : codes) {
      checkCode(code);
    }
  }

  /**
   * Check code.
   *
   * @param code the code
   */
  private void checkCode(final String code) {
    // codes should not contain spaces or parentheses
    if (code.matches(".*[ \\t\\(\\)].*")) {
      throw new RuntimeException("Concept code contains whitespace or parens = " + code);
    }
  }
}