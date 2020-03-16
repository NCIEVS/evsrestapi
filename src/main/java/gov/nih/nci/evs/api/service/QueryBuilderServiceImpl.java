package gov.nih.nci.evs.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.properties.StardogProperties;

@Service
@PropertySource("classpath:sparql-queries.properties")
public class QueryBuilderServiceImpl implements QueryBuilderService {

  private static final Logger log =
      LoggerFactory.getLogger(QueryBuilderServiceImpl.class);

  @Autowired
  StardogProperties stardogProperties;

  @Autowired
  Environment env;
  
  public String contructPrefix() {
    String prefix = String.join(System.getProperty("line.separator"),
        "PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/"
            + stardogProperties.getOwlfileName() + "#>",
        "PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/"
            + stardogProperties.getOwlfileName() + ">",
        "PREFIX owl:<http://www.w3.org/2002/07/owl#>",
        "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
        "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>",
        "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>",
        "PREFIX dc:<http://purl.org/dc/elements/1.1/>",
        "PREFIX xml:<http://www.w3.org/2001/XMLSchema#>");

    log.debug("prefix - " + prefix);
    return prefix;
  }

  public String constructClassCountsQuery(String namedGraph) {
    String query = String.format(env.getProperty("class.counts"), namedGraph);

    log.debug("constructGetClassCounts - " + query);
    return query;
  }

  public String constructAllGraphNamesQuery() {
    String query = env.getProperty("all.graph.names");
    
    log.debug("constructAllGraphQuery - " + query);
    return query;
  }

  /**
   * Return the SPARQL VersionInfo Query
   * 
   * @param namedGraph Named graph.
   * @return SPARQL Version Info query
   */
  public String constructVersionInfoQuery(String namedGraph) {
    String query = String.format(env.getProperty("version.info"), namedGraph);

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
    String query = String.format(env.getProperty("axiom.qualifier"), propertyCode, namedGraph);

    log.debug("constructAxiomQualiferQuery - " + query);
    return query;
  }

  public String constructPropertyQuery(String conceptCode, String namedGraph) {
    String query = String.format(env.getProperty("property"), conceptCode, namedGraph);

    log.debug("constructPropertyQuery - " + query);
    return query;
  }
  
  public String constructPropertyNoRestrictionsQuery(String conceptCode,
    String namedGraph) {
    String query = String.format(env.getProperty("property.no.restrictions"), conceptCode, namedGraph);
    
    log.debug("constructPropertyNoRestrictionsQuery - " + query);
    return query;
  }

  public String constructAllPropertyQuery(String conceptCode,
    String namedGraph) {
    String query = String.format(env.getProperty("all.property"), conceptCode, namedGraph);

    log.debug("constructPropertyQuery - " + query);
    return query;
  }

  public String constructConceptLabelQuery(String conceptCode,
    String namedGraph) {
    String query = String.format(env.getProperty("concept.label"), conceptCode, namedGraph);

    log.debug("constructConceptLabelQuery - " + query);
    return query;
  }

  public String constructAxiomQuery(String conceptCode, String namedGraph) {
    String query = String.format(env.getProperty("axiom"), conceptCode, namedGraph);

    log.debug("constructAxiomQuery - " + query);
    return query;
  }

  public String constructSubconceptQuery(String conceptCode,
    String namedGraph) {
    String query = String.format(env.getProperty("subconcept"), conceptCode, namedGraph);

    log.debug("constructSubconceptQuery - " + query);
    return query;
  }

  public String constructSuperconceptQuery(String conceptCode,
    String namedGraph) {
    String query = String.format(env.getProperty("superconcept"), conceptCode, namedGraph);

    log.debug("constructSuperconceptQuery - " + query);
    return query;
  }

  public String constructAssociationsQuery(String conceptCode,
    String namedGraph) {
    String query = String.format(env.getProperty("associations"), conceptCode, namedGraph);

    log.debug("constructAssociationsQuery - " + query);
    return query;
  }

  public String constructInverseAssociationsQuery(String conceptCode,
    String namedGraph) {
    String query = String.format(env.getProperty("inverse.associations"), conceptCode, namedGraph);

    log.debug("constructInverseAssociationsQuery - " + query);
    return query;
  }

  public String constructInverseRolesQuery(String conceptCode,
    String namedGraph) {
    String query = String.format(env.getProperty("inverse.roles"), conceptCode, namedGraph);

    log.debug("constructInverseRolesQuery - " + query);
    return query;
  }

  public String constructRolesQuery(String conceptCode, String namedGraph) {
    String query = String.format(env.getProperty("roles"), conceptCode, namedGraph);
    
    log.debug("constructRolesQuery - " + query);
    return query;
  }

  public String constructDisjointWithQuery(String conceptCode,
    String namedGraph) {
    String query = String.format(env.getProperty("disjoint.with"), conceptCode, namedGraph);
    
    log.debug("constructDisjointWithQuery - " + query);
    return query;
  }

  public String constructHierarchyQuery(String namedGraph) {
    String query = String.format(env.getProperty("hierarchy"), namedGraph);

    log.debug("constructHierarchyQuery - " + query);
    return query;
  }

  public String constructAllPropertiesQuery(String namedGraph) {
    String query = String.format(env.getProperty("all.properties"), namedGraph);

    log.debug("constructAllPropertiesQuery - " + query);
    return query;
  }

  public String constructAllAssociationsQuery(String namedGraph) {
    String query = String.format(env.getProperty("all.associations"), namedGraph);
    
    log.debug("constructAllAssociationsQuery - " + query);
    return query;
  }

  public String constructAllRolesQuery(String namedGraph) {
    String query = String.format(env.getProperty("all.roles"), namedGraph);

    log.debug("constructAllRolesQuery - " + query);
    return query;
  }

  public String constructUniqueSourcesQuery(String namedGraph) {
    String query = String.format(env.getProperty("unique.sources"), namedGraph);

    log.debug("constructUniqueSourcesQuery - " + query);
    return query;
  }
}