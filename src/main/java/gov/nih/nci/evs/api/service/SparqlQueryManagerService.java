
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.evs.EvsAssociation;
import gov.nih.nci.evs.api.model.evs.EvsAxiom;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsMapsTo;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsRelatedConcept;
import gov.nih.nci.evs.api.model.evs.EvsVersionInfo;
import gov.nih.nci.evs.api.model.evs.HierarchyNode;
import gov.nih.nci.evs.api.model.evs.Path;
import gov.nih.nci.evs.api.model.evs.Paths;
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
  public EvsConcept getEvsConceptByCode(String conceptCode, Terminology terminology, IncludeParam ip)
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
  public List<EvsConcept> getAllProperties(Terminology terminology, IncludeParam ip)
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
  public List<EvsConcept> getAllAssociations(Terminology terminology, IncludeParam ip)
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
  public List<EvsConcept> getAllRoles(Terminology terminology, IncludeParam ip)
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
  public EvsConcept getEvsProperty(String conceptCode, Terminology terminology, IncludeParam param)
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
  public List<EvsAssociation> getEvsAssociations(String conceptCode, Terminology terminology)
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
  public List<EvsAssociation> getEvsInverseAssociations(String conceptCode, Terminology terminology)
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
  public List<EvsAssociation> getEvsRoles(String conceptCode, Terminology terminology)
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
  public List<EvsAssociation> getEvsInverseRoles(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs subconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param outputType the output type
   * @return the evs subconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsRelatedConcept> getEvsSubconcepts(String conceptCode, Terminology terminology,
    String outputType) throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs superconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param outputType the output type
   * @return the evs superconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsRelatedConcept> getEvsSuperconcepts(String conceptCode, Terminology terminology,
    String outputType) throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs maps to.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs maps to
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsMapsTo> getEvsMapsTo(String conceptCode, Terminology terminology) throws IOException;

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
   * @return the list of {@link EvsVersionInfo} objects
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsVersionInfo> getEvsVersionInfoList()
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
  public EvsVersionInfo getEvsVersionInfo(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the evs concept by label properties.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param properties the properties
   * @return the evs concept by label properties
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public EvsConcept getEvsConceptByLabelProperties(String conceptCode, Terminology terminology,
    List<String> properties) throws JsonMappingException, JsonParseException, IOException;

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
   * Returns the concept properties.
   *
   * @param evsConcept the evs concept
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param outputType the output type
   * @param propertyList the property list
   * @return the concept properties
   * @throws IOException Signals that an I/O exception has occurred.
   */
  void getConceptProperties(EvsConcept evsConcept, String conceptCode, Terminology terminology, String outputType,
      List<String> propertyList) throws IOException;

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
  void getConcept(EvsConcept evsConcept, String conceptCode, Terminology terminology, IncludeParam ip)
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
  void getProperty(EvsConcept evsConcept, String conceptCode, Terminology terminology, IncludeParam ip)
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
  List<EvsProperty> getEvsProperties(String conceptCode, Terminology terminology)
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
  List<EvsProperty> getEvsPropertiesNoRestrictions(String conceptCode, Terminology terminology)
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
  List<EvsAssociation> getEvsDisjointWith(String conceptCode, Terminology terminology)
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
  List<EvsAxiom> getEvsAxioms(String conceptCode, Terminology terminology)
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
