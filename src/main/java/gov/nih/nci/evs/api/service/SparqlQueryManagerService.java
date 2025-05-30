package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.AssociationEntry;
import gov.nih.nci.evs.api.model.Axiom;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.DisjointWith;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.util.VersionInfo;

/** Sparql query manager service. */
public interface SparqlQueryManagerService {

  /**
   * Returns the concept by code.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param ip the ip
   * @return the concept by code
   * @throws Exception the exception
   */
  public Concept getConcept(String conceptCode, Terminology terminology, IncludeParam ip)
      throws Exception;

  public Concept getConceptFromOpensearch(
      String conceptCode, Terminology terminology, String include) throws Exception;

  /**
   * Returns the all properties.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all properties
   * @throws Exception the exception
   */
  public List<Concept> getAllProperties(Terminology terminology, IncludeParam ip) throws Exception;

  /**
   * Returns the all properties never used.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all properties never used
   * @throws Exception the exception
   */
  public List<Concept> getNeverUsedProperties(Terminology terminology, IncludeParam ip)
      throws Exception;

  /**
   * Returns the all remodeled properties.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all remodeled properties
   * @throws Exception the exception
   */
  public List<Concept> getRemodeledProperties(Terminology terminology, IncludeParam ip)
      throws Exception;

  /**
   * Returns the distinct property values.
   *
   * @param terminology the terminology
   * @param propertyCode the property code
   * @return the distinct property values
   * @throws Exception the exception
   */
  public List<String> getDistinctPropertyValues(Terminology terminology, String propertyCode)
      throws Exception;

  /**
   * Returns the remodeled qualifiers.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the remodeled qualifiers
   * @throws Exception the exception
   */
  public List<Concept> getRemodeledQualifiers(Terminology terminology, IncludeParam ip)
      throws Exception;

  /**
   * Returns the axiom qualifiers list.
   *
   * @param propertyCode the property code
   * @param terminology the terminology
   * @return the axiom qualifiers list
   * @throws Exception the exception
   */
  public List<String> getQualifierValues(String propertyCode, Terminology terminology)
      throws Exception;

  /**
   * Returns the subset members.
   *
   * @param subsetCode the subset code
   * @param terminology the terminology
   * @return the subset members
   * @throws Exception the exception
   */
  public List<Concept> getSubsetMembers(String subsetCode, Terminology terminology)
      throws Exception;

  /**
   * Returns the all associations.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all associations
   * @throws Exception the exception
   */
  public List<Concept> getAllAssociations(Terminology terminology, IncludeParam ip)
      throws Exception;

  /**
   * Returns the all roles.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all roles
   * @throws Exception the exception
   */
  public List<Concept> getAllRoles(Terminology terminology, IncludeParam ip) throws Exception;

  /**
   * Returns the all synonym types.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all synonym types
   * @throws Exception the exception
   */
  public List<Concept> getAllSynonymTypes(Terminology terminology, IncludeParam ip)
      throws Exception;

  /**
   * Returns the all definition types.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all definition types
   * @throws Exception the exception
   */
  public List<Concept> getAllDefinitionTypes(Terminology terminology, IncludeParam ip)
      throws Exception;

  /**
   * Returns the property.
   *
   * @param code the code
   * @param terminology the terminology
   * @param param the param
   * @return the property
   * @throws Exception the exception
   */
  public Concept getProperty(String code, Terminology terminology, IncludeParam param)
      throws Exception;

  /**
   * Returns the qualifier.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param param the param
   * @return the qualifier
   * @throws Exception the exception
   */
  public Concept getQualifier(String conceptCode, Terminology terminology, IncludeParam param)
      throws Exception;

  /**
   * Returns the association.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param param the param
   * @return the association
   * @throws Exception the exception
   */
  public Concept getAssociation(String conceptCode, Terminology terminology, IncludeParam param)
      throws Exception;

  /**
   * Returns the role.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param param the param
   * @return the role
   * @throws Exception the exception
   */
  public Concept getRole(String conceptCode, Terminology terminology, IncludeParam param)
      throws Exception;

  /**
   * Returns the associations.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the associations
   * @throws Exception the exception
   */
  public List<Association> getAssociations(String conceptCode, Terminology terminology)
      throws Exception;

  /**
   * Returns the associations for all codes.
   *
   * @param terminology the terminology
   * @param inverseFlag the inverse flag
   * @return the associations for all codes
   * @throws Exception the exception
   */
  public Map<String, List<Association>> getAssociationsForAllCodes(
      Terminology terminology, boolean inverseFlag) throws Exception;

  /**
   * Returns the inverse associations.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the inverse associations
   * @throws Exception the exception
   */
  public List<Association> getInverseAssociations(String conceptCode, Terminology terminology)
      throws Exception;

  /**
   * Returns the roles.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the roles
   * @throws Exception the exception
   */
  public List<Role> getRoles(String conceptCode, Terminology terminology) throws Exception;

  /**
   * Returns the complex roles for all codes.
   *
   * @param terminology the terminology
   * @param inverseFlag the inverse flag
   * @return the complex roles for all codes
   * @throws Exception the exception
   */
  public Map<String, List<Role>> getComplexRolesForAllCodes(
      Terminology terminology, boolean inverseFlag) throws Exception;

  /**
   * Returns the inverse roles.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the inverse roles
   * @throws Exception the exception
   */
  public List<Role> getInverseRoles(String conceptCode, Terminology terminology) throws Exception;

  /**
   * Returns the subconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the subconcepts
   * @throws Exception the exception
   */
  public List<Concept> getChildren(String conceptCode, Terminology terminology) throws Exception;

  /**
   * Returns the superconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the superconcepts
   * @throws Exception the exception
   */
  public List<Concept> getParents(String conceptCode, Terminology terminology) throws Exception;

  /**
   * Returns the maps to.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the maps to
   * @throws Exception the exception
   */
  public List<Mapping> getMapsTo(String conceptCode, Terminology terminology) throws Exception;

  /**
   * Returns the root nodes.
   *
   * @param terminology the terminology
   * @return the root nodes
   * @throws Exception the exception
   */
  public List<HierarchyNode> getRootNodes(Terminology terminology) throws Exception;

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param terminology the terminology
   * @return the child nodes
   * @throws Exception the exception
   */
  public List<HierarchyNode> getChildNodes(String parent, Terminology terminology) throws Exception;

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param maxLevel the max level
   * @param terminology the terminology
   * @return the child nodes
   * @throws Exception the exception
   */
  public List<HierarchyNode> getChildNodes(String parent, int maxLevel, Terminology terminology)
      throws Exception;

  /**
   * Returns the all graph names.
   *
   * @return the all graph names
   * @throws Exception the exception
   */
  public List<String> getAllGraphNames() throws Exception;

  /**
   * Returns Version Information objects for all graphs loaded in db.
   *
   * @param db the db
   * @return the list of {@link VersionInfo} objects
   * @throws Exception the exception
   * @throws ParseException the parse exception
   */
  public List<Terminology> getTerminologies(String db) throws Exception, ParseException;

  /**
   * Returns the definition sources.
   *
   * @param terminology the terminology
   * @return the definition sources
   * @throws Exception the exception
   */
  public List<ConceptMinimal> getDefinitionSources(Terminology terminology) throws Exception;

  /**
   * Returns the synonym sources.
   *
   * @param terminology the terminology
   * @return the synonym sources
   * @throws Exception the exception
   */
  public List<ConceptMinimal> getSynonymSources(Terminology terminology) throws Exception;

  /**
   * Returns the term types.
   *
   * @param terminology the terminology
   * @return the term types
   * @throws Exception the exception
   */
  public List<ConceptMinimal> getTermTypes(Terminology terminology) throws Exception;

  /**
   * Returns the named graph.
   *
   * @param terminology the terminology
   * @return the named graph
   */
  String getNamedGraph(Terminology terminology);

  /**
   * Returns the query URL.
   *
   * @return the query URL
   */
  String getQueryURL();

  /**
   * Returns the concepts.
   *
   * @param concepts the concepts
   * @param terminology the terminology
   * @param hierarchy the hierarchy
   * @return the concepts
   * @throws Exception the exception
   */
  List<Concept> getConcepts(
      List<Concept> concepts, Terminology terminology, HierarchyUtils hierarchy) throws Exception;

  /**
   * Returns the properties.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the properties
   * @throws Exception the exception
   */
  List<Property> getProperties(String conceptCode, Terminology terminology) throws Exception;

  /**
   * Returns the disjoint with.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the disjoint with
   * @throws Exception the exception
   */
  List<DisjointWith> getDisjointWith(String conceptCode, Terminology terminology) throws Exception;

  /**
   * Returns the axioms.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param qualifierFlag the qualifier flag - used to avoid Q-P-Q-P infinite loop
   * @return the axioms
   * @throws Exception the exception
   */
  List<Axiom> getAxioms(String conceptCode, Terminology terminology, boolean qualifierFlag)
      throws Exception;

  /**
   * Returns the main type hierarchy.
   *
   * @param terminology the terminology
   * @param mainTypeSet the main type set
   * @param broadCategorySet the broad category set
   * @param hierarchy the hierarchy
   * @return the main type hierarchy
   * @throws Exception the exception
   */
  Map<String, Paths> getMainTypeHierarchy(
      Terminology terminology,
      Set<String> mainTypeSet,
      Set<String> broadCategorySet,
      final HierarchyUtils hierarchy)
      throws Exception;

  /**
   * Returns the all child nodes.
   *
   * @param parent the parent
   * @param terminology the terminology
   * @return the all child nodes
   * @throws Exception the exception
   */
  List<String> getAllChildNodes(String parent, Terminology terminology) throws Exception;

  /**
   * gets all concepts (minimal).
   *
   * @param terminology the terminology
   * @return list of concept objects
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Concept> getAllConceptsWithCode(Terminology terminology) throws IOException;

  /**
   * Returns the all concepts without code.
   *
   * @param terminology the terminology
   * @return the all concepts without code
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws Exception
   */
  public List<Concept> getAllConceptsWithoutCode(Terminology terminology)
      throws IOException, Exception;

  /**
   * Returns the path in hierarchy.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the path in hierarchy
   * @throws Exception the exception
   */
  public List<HierarchyNode> getPathInHierarchy(String code, Terminology terminology)
      throws Exception;

  /**
   * checks path in hierarchy.
   *
   * @param code the code
   * @param node the HierarchyNode
   * @param path the path to check
   * @param terminology the terminology
   * @return N/A
   * @throws Exception the exception
   */
  void checkPathInHierarchy(String code, HierarchyNode node, Path path, Terminology terminology)
      throws Exception;

  /**
   * gets all subsets.
   *
   * @param terminology the terminology
   * @return list of concept objects
   * @throws Exception the exception
   */
  List<Concept> getAllSubsets(Terminology terminology) throws Exception;

  /**
   * gets association entries.
   *
   * @param terminology the terminology
   * @param association the association
   * @return list of AssociationEntries
   * @throws Exception
   */
  public List<AssociationEntry> getAssociationEntries(Terminology terminology, Concept association)
      throws Exception;

  /**
   * Helper method for calling the get Hierarchy with caching enabled
   *
   * @param terminology
   * @return
   * @throws Exception
   */
  public HierarchyUtils getHierarchyUtilsCache(final Terminology terminology) throws Exception;

  /**
   * Helper method for call the get all qualifiers with caching enabled
   *
   * @param terminology
   * @return
   * @throws Exception
   */
  public List<Concept> getAllQualifiersCache(Terminology terminology, IncludeParam ip)
      throws Exception;
}
