
package gov.nih.nci.evs.api.service;

import java.io.IOException;
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
import gov.nih.nci.evs.api.model.VersionInfo;
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
   * Returns the evs concept by code.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param ip the ip
   * @return the evs concept by code
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Concept getEvsConceptByCode(String conceptCode, Terminology terminology, IncludeParam ip)
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
   * Returns the evs property.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param param the param
   * @return the evs property
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Concept getEvsProperty(String conceptCode, Terminology terminology, IncludeParam param)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs associations.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Association> getEvsAssociations(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs inverse associations.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs inverse associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Association> getEvsInverseAssociations(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs roles.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Role> getEvsRoles(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs inverse roles.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs inverse roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Role> getEvsInverseRoles(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs subconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs subconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Concept> getEvsSubconcepts(String conceptCode, Terminology terminology) throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs superconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs superconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<Concept> getEvsSuperconcepts(String conceptCode, Terminology terminology) throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs maps to.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs maps to
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<gov.nih.nci.evs.api.model.Map> getEvsMapsTo(String conceptCode, Terminology terminology) throws IOException;

  /**
   * Returns the root nodes.
   *
   * @param terminology the terminology
   * @return the root nodes
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  public List<HierarchyNode> getRootNodes(Terminology terminology) throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param terminology the terminology
   * @return the child nodes
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  public List<HierarchyNode> getChildNodes(String parent, Terminology terminology) throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param maxLevel the max level
   * @param terminology the terminology
   * @return the child nodes
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  public List<HierarchyNode> getChildNodes(String parent, int maxLevel, Terminology terminology) throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the path in hierarchy.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the path in hierarchy
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  public List<HierarchyNode> getPathInHierarchy(String code, Terminology terminology) throws JsonParseException, JsonMappingException, IOException;

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
  public Paths getPathToParent(String conceptCode, String parentConceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the all graph names.
   *
   * @param terminology the terminology
   * @return the all graph names
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<String> getAllGraphNames()
    throws JsonParseException, JsonMappingException, IOException;
  
  /**
   * Returns EvsVersion Information objects for all graphs loaded in db
   * 
   * @return the list of {@link VersionInfo} objects
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<VersionInfo> getEvsVersionInfoList()
    throws JsonParseException, JsonMappingException, IOException;
  
  /**
   * Returns the evs version info.
   *
   * @param terminology the terminology
   * @return the evs version info
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public VersionInfo getEvsVersionInfo(Terminology terminology)
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
   * Returns the evs concept label.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs concept label
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  String getEvsConceptLabel(String conceptCode, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the concept.
   *
   * @param evsConcept the evs concept
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param ip the ip
   * @return the concept
   * @throws IOException Signals that an I/O exception has occurred.
   */
  void getConcept(Concept evsConcept, String conceptCode, Terminology terminology, IncludeParam ip)
      throws IOException;

  /**
   * Returns the property.
   *
   * @param evsConcept the evs concept
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param ip the ip
   * @return the property
   * @throws IOException Signals that an I/O exception has occurred.
   */
  void getProperty(Concept evsConcept, String conceptCode, Terminology terminology, IncludeParam ip)
      throws IOException;

  /**
   * Returns the evs properties.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs properties
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<Property> getEvsProperties(String conceptCode, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs properties no restrictions.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs properties no restrictions
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<Property> getEvsPropertiesNoRestrictions(String conceptCode, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs disjoint with.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs disjoint with
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<DisjointWith> getEvsDisjointWith(String conceptCode, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs axioms.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs axioms
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<Axiom> getEvsAxioms(String conceptCode, Terminology terminology)
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
  ArrayList<String> getHierarchy(Terminology terminology) throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the all child nodes.
   *
   * @param parent the parent
   * @param terminology the terminology
   * @return the all child nodes
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  List<String> getAllChildNodes(String parent, Terminology terminology) throws JsonParseException, JsonMappingException, IOException;

  /**
   * Check path in hierarchy.
   *
   * @param code the code
   * @param node the node
   * @param path the path
   * @param terminology the terminology
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  void checkPathInHierarchy(String code, HierarchyNode node, Path path, Terminology terminology) throws JsonParseException, JsonMappingException, IOException;

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
   * Get hiearchy for a given terminology
   * 
   * @param terminology the terminology
   * @return the hierarchy
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  public HierarchyUtils getHierarchyUtils(Terminology terminology) throws JsonParseException, JsonMappingException, IOException;

  /**
   * Get paths
   * 
   * @param terminology the terminology
   * @return paths
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  Paths getPaths(Terminology terminology) throws JsonParseException, JsonMappingException, IOException;
  
  /**
   * Get list of terminologies
   * 
   * @return the list of terminologies
   * @throws IOException
   */
  List<Terminology> getTerminologies() throws Exception;
}
