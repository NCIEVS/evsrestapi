package gov.nih.nci.evs.api.service;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
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

/**
 * @author Arun
 *
 */
@Service
@PropertySource("classpath:sparql-queries.properties")
public class QueryBuilderServiceImpl implements QueryBuilderService {

  private static final Logger log =
      LoggerFactory.getLogger(QueryBuilderServiceImpl.class);

  @Autowired
  StardogProperties stardogProperties;

  @Autowired
  Environment env;
  
  public String contructPrefix(String source) {
    String prefix = env.getProperty("prefix.common");
    
    if (StringUtils.isNotEmpty(source)) {
      Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
          source);
      prefix = getResolvedProperty("prefix.graph", values) + System.getProperty("line.separator") + prefix;
    }

    log.debug("prefix - " + prefix);
    return prefix;
  }

  public String constructClassCountsQuery(String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        namedGraph);
    String query = getResolvedProperty("class.counts", values);

    log.debug("constructGetClassCounts - " + query);
    return query;
  }

  public String constructAllGraphNamesQuery() {
    String query = env.getProperty("all.graph.names");
    
    log.debug("constructAllGraphQuery - " + query);
    return query;
  }

  /**
   * Construct all graph names and corresponding ontology versions (limited to owl ontologies)
   * 
   * Properties key is {@code all.graphs.and.versions}
   * 
   * @return
   */
  public String constructAllGraphsAndVersionsQuery() {
    String query = env.getProperty("all.graphs.and.versions");
    
    log.debug("constructAllGraphsAndVersionsQuery - " + query);
    return query;
  }
  
  /**
   * Return the SPARQL VersionInfo Query
   * 
   * @param namedGraph Named graph.
   * @return SPARQL Version Info query
   */
  public String constructVersionInfoQuery(String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        namedGraph);
    String query = getResolvedProperty("version.info", values);
    
    log.debug("constructVersionInfoQuery - " + query);
    return query;
  }

  /*
   * Keep for reference, counts may come in handy later public String
   * constructAxiomQualifierCountQuery(String propertyCode, String namedGraph) {
   * StringBuffer query = new StringBuffer(); query.
   * append("SELECT ?axiomQualifier (COUNT(?axiomQualifier) as (COUNT(!axiomQualifier) as?count)\n"
   * ); query.append("{ GRAPH <" + namedGraph + ">");
   * query.append("  { ?axiom a owl:Axiom .\n"); query.append("    ?axiom :" +
   * propertyCode + "?axiomQualifier\n"); query.append("  }\n");
   * query.append("}\n"); query.append("GROUP BY ?axiomQualifier\n");
   * query.append("ORDER BY ?axiomQualifier\n");
   * 
   * log.debug("constructAxiomQualifierCountQuery - " + query.toString());
   * return query.toString(); }
   */

  public String constructAxiomQualifierQuery(String propertyCode,
    String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        propertyCode, namedGraph);
    String query = getResolvedProperty("axiom.qualifier", values);

    log.debug("constructAxiomQualiferQuery - " + query);
    return query;
  }

  public String constructPropertyQuery(String conceptCode, String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("property", values);

    log.debug("constructPropertyQuery - " + query);
    return query;
  }
  
  public String constructPropertyNoRestrictionsQuery(String conceptCode,
    String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("property.no.restrictions", values);
    
    log.debug("constructPropertyNoRestrictionsQuery - " + query);
    return query;
  }

  public String constructAllPropertyQuery(String conceptCode,
    String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("all.property", values);

    log.debug("constructPropertyQuery - " + query);
    return query;
  }


  public String constructConceptLabelQuery(String conceptCode,
    String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("concept.label", values);

    log.debug("constructConceptLabelQuery - " + query);
    return query;
  }

  public String constructAxiomQuery(String conceptCode, String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("axiom", values);

    log.debug("constructAxiomQuery - " + query);
    return query;
  }

  public String constructSubconceptQuery(String conceptCode,
    String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("subconcept", values);

    log.debug("constructSubconceptQuery - " + query);
    return query;
  }

  public String constructSuperconceptQuery(String conceptCode,
    String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("superconcept", values);

    log.debug("constructSuperconceptQuery - " + query);
    return query;
  }

  public String constructAssociationsQuery(String conceptCode,
    String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("associations", values);

    log.debug("constructAssociationsQuery - " + query);
    return query;
  }

  public String constructInverseAssociationsQuery(String conceptCode,
    String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("inverse.associations", values);

    log.debug("constructInverseAssociationsQuery - " + query);
    return query;
  }

  public String constructInverseRolesQuery(String conceptCode,
    String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("inverse.roles", values);

    log.debug("constructInverseRolesQuery - " + query);
    return query;
  }

  public String constructRolesQuery(String conceptCode, String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("roles", values);
    
    log.debug("constructRolesQuery - " + query);
    return query;
  }

  public String constructDisjointWithQuery(String conceptCode,
    String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        conceptCode, namedGraph);
    String query = getResolvedProperty("disjoint.with", values);
    
    log.debug("constructDisjointWithQuery - " + query);
    return query;
  }

  public String constructHierarchyQuery(String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        namedGraph);
    String query = getResolvedProperty("hierarchy", values);

    log.debug("constructHierarchyQuery - " + query);
    return query;
  }

  public String constructAllPropertiesQuery(String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        namedGraph);
    String query = getResolvedProperty("all.properties", values);
    log.debug("constructAllPropertiesQuery - " + query);
    return query;
  }

  public String constructAllPropertiesNeverUsedQuery(String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        namedGraph);
    String query = getResolvedProperty("all.propertiesNeverUsed", values);
    log.debug("constructAllPropertiesNeverUsedQuery - " + query);
    return query;
  }

  public String constructAllQualifiersQuery(String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        namedGraph);
    String query = getResolvedProperty("all.qualifiers", values);
    log.debug("constructAllQualifiersQuery - " + query);
    return query;
  }

  public String constructAllAssociationsQuery(String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        namedGraph);
    String query = getResolvedProperty("all.associations", values);
    
    log.debug("constructAllAssociationsQuery - " + query);
    return query;
  }

  public String constructAllRolesQuery(String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        namedGraph);
    String query = getResolvedProperty("all.roles", values);

    log.debug("constructAllRolesQuery - " + query);
    return query;
  }

  public String constructUniqueSourcesQuery(String namedGraph) {
    Map<String, String> values = getParamValueMap(new Object() {}.getClass().getEnclosingMethod(),
        namedGraph);
    String query = getResolvedProperty("unique.sources", values);

    log.debug("constructUniqueSourcesQuery - " + query);
    return query;
  }

  private Map<String, String> getParamValueMap(Method m, String ...values) {
    Parameter[] params = m.getParameters();
    Map<String, String> paramMap = new HashMap<>();
    if (params.length == 0) return paramMap;
    if (values == null || values.length != params.length) 
      throw new RuntimeException(String.format("Values length does not parameters length for %s!", m.getName()));
    for(int i=0; i<params.length; i++) {
      paramMap.put(params[i].getName(), values[i]);
    }
    
    if (log.isDebugEnabled()) log.debug("paramMap - " + paramMap);
    
    return paramMap;
  }

  private String getResolvedProperty(String queryKey, Map<String, String> values) {
    return StringSubstitutor.replace(env.getProperty(queryKey), values, "#{", "}");
  }
  
}