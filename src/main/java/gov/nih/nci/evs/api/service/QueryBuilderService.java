
package gov.nih.nci.evs.api.service;

/**
 * Query builder service.
 */
public interface QueryBuilderService {

  /**
   * Construct all property query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructAllPropertyQuery(String conceptCode, String namedGraph);

  /**
   * Construct all graph names query.
   *
   * @return the string
   */
  public String constructAllGraphNamesQuery();

  /**
   * Construct all graph names and corresponding ontology versions (limited to owl ontologies)
   * 
   * Properties key is {@code all.graphs.and.versions}
   * 
   * @return
   */
  public String constructAllGraphsAndVersionsQuery();
  
  /**
   * Construct axiom query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructAxiomQuery(String conceptCode, String namedGraph);

  /**
   * Construct axiom qualifier query.
   *
   * @param propertyCode the property code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructAxiomQualifierQuery(String propertyCode, String namedGraph);

  /**
   * Construct class counts query.
   *
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructClassCountsQuery(String namedGraph);

  /**
   * Construct concept label query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructConceptLabelQuery(String conceptCode, String namedGraph);

  /**
   * Contruct prefix.
   *
   * @param source the graph source
   * @return the string
   */
  public String contructPrefix(String source);

  /**
   * Construct property query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructPropertyQuery(String conceptCode, String namedGraph);

  /**
   * Construct property no restrictions query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructPropertyNoRestrictionsQuery(String conceptCode, String namedGraph);

  /**
   * Construct subconcept query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructSubconceptQuery(String conceptCode, String namedGraph);

  /**
   * Construct superconcept query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructSuperconceptQuery(String conceptCode, String namedGraph);

  /**
   * Construct associations query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructAssociationsQuery(String conceptCode, String namedGraph);

  /**
   * Construct inverse associations query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructInverseAssociationsQuery(String conceptCode, String namedGraph);

  /**
   * Construct roles query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructRolesQuery(String conceptCode, String namedGraph);

  /**
   * Construct inverse roles query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructInverseRolesQuery(String conceptCode, String namedGraph);

  /**
   * Construct disjoint with query.
   *
   * @param conceptCode the concept code
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructDisjointWithQuery(String conceptCode, String namedGraph);

  /**
   * Construct hierarchy query.
   *
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructHierarchyQuery(String namedGraph);

  /**
   * Construct all properties query.
   *
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructAllPropertiesQuery(String namedGraph);

  /**
   * Construct all associations query.
   *
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructAllAssociationsQuery(String namedGraph);

  /**
   * Construct all roles query.
   *
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructAllRolesQuery(String namedGraph);

  /**
   * Construct version info query.
   *
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructVersionInfoQuery(String namedGraph);

  /**
   * Construct unique sources query.
   *
   * @param namedGraph the named graph
   * @return the string
   */
  public String constructUniqueSourcesQuery(String namedGraph);
}
