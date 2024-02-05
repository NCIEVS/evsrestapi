package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.util.ConceptUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Reference implementation of {@link QueryBuilderService}. Includes hibernate tags for MEME
 * database.
 */
@Service
@PropertySource("classpath:sparql-queries.properties")
public class QueryBuilderServiceImpl implements QueryBuilderService {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(QueryBuilderServiceImpl.class);

  /** The stardog properties. */
  @Autowired StardogProperties stardogProperties;

  /** The env. */
  @Autowired Environment env;

  /**
   * Contruct prefix.
   *
   * @param terminology the terminology
   * @return the string
   */
  @Override
  public String constructPrefix(final Terminology terminology) {

    // Always use common
    String prefix = env.getProperty("prefix.common");

    // Only do the next part if terminology is set
    if (terminology != null) {

      final Map<String, String> values = ConceptUtils.asMap("source", terminology.getSource());

      // Try terminology-specific prefix
      if (env.containsProperty("prefix." + terminology.getTerminology())) {
        prefix =
            getResolvedProperty("prefix." + terminology.getTerminology(), values)
                + System.getProperty("line.separator")
                + prefix;
      }

      // Otherwise use prefix.graph + prefix.common
      else {
        prefix =
            getResolvedProperty("prefix.graph", values)
                + System.getProperty("line.separator")
                + prefix;
      }
    }

    // log.debug("prefix - " + prefix);
    return prefix;
  }

  /**
   * Construct graph query.
   *
   * @param queryProp the query prop
   * @return the string
   */
  @Override
  public String constructGraphQuery(final String queryProp, final List<String> ignoreSources) {
    if (CollectionUtils.isNotEmpty(ignoreSources)) {
      String strIgnoreSources = ignoreSources.stream().map(s -> "<" + s + ">,").collect(Collectors.joining());
      strIgnoreSources = strIgnoreSources.substring(0, strIgnoreSources.lastIndexOf(','));
      String query =
              getResolvedProperty(queryProp, Map.of("ignoredSources", strIgnoreSources));
      return query;
    } else {
      return env.getProperty(queryProp);
    }
  }

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param terminology the terminology
   * @return the string
   */
  @Override
  public String constructQuery(final String queryProp, final Terminology terminology) {
    Map<String, String> values =
        ConceptUtils.asMap(
            "codeCode",
            terminology.getMetadata().getCode(),
            "namedGraph",
            terminology.getGraph(),
            "preferredNameCode",
            terminology.getMetadata().getPreferredName());
    final String queryPropTerminology = queryProp + "." + terminology.getTerminology();
    String query =
        getResolvedProperty(
            env.containsProperty(queryPropTerminology) ? queryPropTerminology : queryProp, values);
    // log.debug("construct " + queryProp + " - " + query);
    return query;
  }

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param terminology the terminology
   * @param conceptCode the concept code
   * @return the string
   */
  @Override
  public String constructQuery(
      final String queryProp, final Terminology terminology, final String conceptCode) {
    checkCode(conceptCode);
    final Map<String, String> values =
        ConceptUtils.asMap(
            "codeCode",
            terminology.getMetadata().getCode(),
            "conceptCode",
            conceptCode,
            "conceptAbout",
            conceptCode,
            "namedGraph",
            terminology.getGraph(),
            "preferredNameCode",
            terminology.getMetadata().getPreferredName());
    final String queryPropTerminology = queryProp + "." + terminology.getTerminology();
    final String query =
        getResolvedProperty(
            env.containsProperty(queryPropTerminology) ? queryPropTerminology : queryProp, values);
    return query;
  }

  /**
   * Construct batch query.
   *
   * @param queryProp the query prop
   * @param terminology the terminology
   * @param conceptCodes the concept codes
   * @return the string
   */
  @Override
  public String constructBatchQuery(
      final String queryProp, final Terminology terminology, final List<String> conceptCodes) {
    final String inClause = getInClause(conceptCodes);
    final String aboutClause = getAboutClause(conceptCodes);
    final Map<String, String> values =
        ConceptUtils.asMap(
            "codeCode",
            terminology.getMetadata().getCode(),
            "namedGraph",
            terminology.getGraph(),
            "inClause",
            inClause,
            "aboutClause",
            aboutClause,
            "preferredNameCode",
            terminology.getMetadata().getPreferredName());
    final String queryPropTerminology = queryProp + "." + terminology.getTerminology();
    if (env.containsProperty(queryPropTerminology)) {
      log.info("    use terminology-specific query = " + queryPropTerminology);
    }
    String query =
        getResolvedProperty(
            env.containsProperty(queryPropTerminology) ? queryPropTerminology : queryProp, values);
    // log.debug("construct " + queryProp + " - " + query);
    return query;
  }

  /**
   * Construct query.
   *
   * @param queryProp the query prop
   * @param terminology the terminology
   * @param values the values
   * @return the string
   */
  @Override
  public String constructQuery(
      String queryProp, Terminology terminology, Map<String, String> values) {
    // Validate codes
    for (final Map.Entry<String, String> entry : values.entrySet()) {
      if (entry.getKey().toLowerCase().contains("code")) {
        checkCode(entry.getValue());
      }
    }
    final String queryPropTerminology = queryProp + "." + terminology.getTerminology();
    String query =
        getResolvedProperty(
            env.containsProperty(queryPropTerminology) ? queryPropTerminology : queryProp, values);
    // log.debug("construct " + queryProp + " - " + query);
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
    return new StringBuilder()
        .append("'")
        .append(
            String.join(
                "','",
                values.stream().filter(v -> !v.startsWith("http")).collect(Collectors.toList())))
        .append("'")
        .toString();
  }

  /**
   * Returns the about clause.
   *
   * @param values the values
   * @return the about clause
   */
  private String getAboutClause(final List<String> values) {
    final String result =
        new StringBuilder()
            .append(
                String.join(
                    ",",
                    values.stream()
                        .filter(v -> v.startsWith("http"))
                        .map(v -> "<" + v + ">")
                        .collect(Collectors.toList())))
            .toString();
    return result.isEmpty() ? "<empty>" : result;
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
