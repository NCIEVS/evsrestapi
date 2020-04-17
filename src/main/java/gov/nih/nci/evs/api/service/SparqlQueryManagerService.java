
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Axiom;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.DisjointWith;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.ConfigData;
import gov.nih.nci.evs.api.util.HierarchyUtils;

/**
 * Sparql query manager service.
 */
public interface SparqlQueryManagerService {

  /**
   * Check concept exists.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return true, if successful
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public boolean checkConceptExists(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the concept by code.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param ip the ip
   * @return the concept by code
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Concept getConcept(String conceptCode, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the all properties.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all properties
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Concept> getAllProperties(Terminology terminology, IncludeParam ip)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the all properties never used.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all properties never used
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Concept> getAllPropertiesNeverUsed(Terminology terminology, IncludeParam ip)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the all qualifiers.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all qualifiers
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Concept> getAllQualifiers(Terminology terminology, IncludeParam ip)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the axiom qualifiers list.
   *
   * @param propertyCode the property code
   * @param terminology the terminology
   * @return the axiom qualifiers list
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<String> getAxiomQualifiersList(String propertyCode, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the all associations.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all associations
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Concept> getAllAssociations(Terminology terminology, IncludeParam ip)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the all roles.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all roles
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Concept> getAllRoles(Terminology terminology, IncludeParam ip)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the property.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param param the param
   * @return the property
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Concept getProperty(String conceptCode, Terminology terminology, IncludeParam param)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the qualifier.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param param the param
   * @return the qualifier
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Concept getQualifier(String conceptCode, Terminology terminology, IncludeParam param)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the association.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param param the param
   * @return the association
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Concept getAssociation(String conceptCode, Terminology terminology, IncludeParam param)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the role.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param param the param
   * @return the role
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Concept getRole(String conceptCode, Terminology terminology, IncludeParam param)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the associations.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Association> getAssociations(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the inverse associations.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the inverse associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Association> getInverseAssociations(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the roles.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Role> getRoles(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the inverse roles.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the inverse roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Role> getInverseRoles(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the subconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the subconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Concept> getSubconcepts(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the superconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the superconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Concept> getSuperconcepts(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the maps to.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the maps to
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<gov.nih.nci.evs.api.model.Map> getMapsTo(String conceptCode, Terminology terminology)
    throws IOException;

  /**
   * Returns the root nodes.
   *
   * @param terminology the terminology
   * @return the root nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<HierarchyNode> getRootNodes(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param terminology the terminology
   * @return the child nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<HierarchyNode> getChildNodes(String parent, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param maxLevel the max level
   * @param terminology the terminology
   * @return the child nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<HierarchyNode> getChildNodes(String parent, int maxLevel, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the path in hierarchy.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the path in hierarchy
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<HierarchyNode> getPathInHierarchy(String code, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the path to root.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the path to root
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Paths getPathToRoot(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the path to parent.
   *
   * @param conceptCode the concept code
   * @param parentConceptCode the parent concept code
   * @param terminology the terminology
   * @return the path to parent
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Paths getPathToParent(String conceptCode, String parentConceptCode,
    Terminology terminology) throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the all graph names.
   *
   * @return the all graph names
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<String> getAllGraphNames()
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns Version Information objects for all graphs loaded in db.
   *
   * @return the list of {@link VersionInfo} objects
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws ParseException the parse exception
   */
  public List<Terminology> getTerminologies()
    throws JsonParseException, JsonMappingException, IOException, ParseException;

  /**
   * Returns the version info.
   *
   * @param terminology the terminology
   * @return the version info
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Terminology getTerminology(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the configuration data.
   *
   * @param terminology the terminology
   * @return the configuration data
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public ConfigData getConfigurationData(Terminology terminology)
    throws JsonMappingException, JsonProcessingException, IOException;

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
   * Returns the concept label.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the concept label
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  String getConceptLabel(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the properties.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the properties
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<Property> getConceptProperties(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the properties no restrictions.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the properties no restrictions
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<Property> getMetadataProperties(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the disjoint with.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the disjoint with
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<DisjointWith> getDisjointWith(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the axioms.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param qualifierFlag the qualifier flag - used to avoid Q-P-Q-P infinite
   *          loop
   * @return the axioms
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<Axiom> getAxioms(String conceptCode, Terminology terminology, boolean qualifierFlag)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the hierarchy.
   *
   * @param terminology the terminology
   * @return the hierarchy
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  ArrayList<String> getHierarchy(Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the all child nodes.
   *
   * @param parent the parent
   * @param terminology the terminology
   * @return the all child nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<String> getAllChildNodes(String parent, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Check path in hierarchy.
   *
   * @param code the code
   * @param node the node
   * @param path the path
   * @param terminology the terminology
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  void checkPathInHierarchy(String code, HierarchyNode node, Path path, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the unique sources list.
   *
   * @param terminology the terminology
   * @return the unique sources list
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<String> getUniqueSourcesList(Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Get hiearchy for a given terminology.
   *
   * @param terminology the terminology
   * @return the hierarchy
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public HierarchyUtils getHierarchyUtils(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Get paths.
   *
   * @param terminology the terminology
   * @return paths
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Paths getPaths(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException;
}
