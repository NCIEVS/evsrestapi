
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.evs.EvsAssociation;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsMapsTo;
import gov.nih.nci.evs.api.model.evs.EvsRelatedConcept;
import gov.nih.nci.evs.api.model.evs.EvsVersionInfo;
import gov.nih.nci.evs.api.model.evs.HierarchyNode;
import gov.nih.nci.evs.api.model.evs.Paths;
import gov.nih.nci.evs.api.support.ConfigData;

/**
 * Sparql query manager service.
 */
public interface SparqlQueryManagerService {

  /**
   * Check concept exists.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @return true, if successful
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public boolean checkConceptExists(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs concept by code.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @param ip the ip
   * @return the evs concept by code
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public EvsConcept getEvsConceptByCode(String conceptCode, String dbType, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the all properties.
   *
   * @param dbType the db type
   * @param ip the ip
   * @return the all properties
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsConcept> getAllProperties(String dbType, IncludeParam ip)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the axiom qualifiers list.
   *
   * @param propertyCode the property code
   * @param dbType the db type
   * @return the axiom qualifiers list
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<String> getAxiomQualifiersList(String propertyCode, String dbType)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the all associations.
   *
   * @param dbType the db type
   * @param ip the ip
   * @return the all associations
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsConcept> getAllAssociations(String dbType, IncludeParam ip)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the all roles.
   *
   * @param dbType the db type
   * @param ip the ip
   * @return the all roles
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsConcept> getAllRoles(String dbType, IncludeParam ip)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the evs property.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @param param the param
   * @return the evs property
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public EvsConcept getEvsProperty(String conceptCode, String dbType, IncludeParam param)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs associations.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @return the evs associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsAssociation> getEvsAssociations(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs inverse associations.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @return the evs inverse associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsAssociation> getEvsInverseAssociations(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs roles.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @return the evs roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsAssociation> getEvsRoles(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs inverse roles.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @return the evs inverse roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsAssociation> getEvsInverseRoles(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs subconcepts.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @param outputType the output type
   * @return the evs subconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsRelatedConcept> getEvsSubconcepts(String conceptCode, String dbType,
    String outputType) throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs superconcepts.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @param outputType the output type
   * @return the evs superconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsRelatedConcept> getEvsSuperconcepts(String conceptCode, String dbType,
    String outputType) throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the evs maps to.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @return the evs maps to
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<EvsMapsTo> getEvsMapsTo(String conceptCode, String dbType) throws IOException;

  /**
   * Returns the root nodes.
   *
   * @param dbType the db type
   * @return the root nodes
   */
  public List<HierarchyNode> getRootNodes(String dbType);

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param dbType the db type
   * @return the child nodes
   */
  public List<HierarchyNode> getChildNodes(String parent, String dbType);

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param maxLevel the max level
   * @param dbType the db type
   * @return the child nodes
   */
  public List<HierarchyNode> getChildNodes(String parent, int maxLevel, String dbType);

  /**
   * Returns the path in hierarchy.
   *
   * @param code the code
   * @param dbType the db type
   * @return the path in hierarchy
   */
  public List<HierarchyNode> getPathInHierarchy(String code, String dbType);

  /**
   * Returns the path to root.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @return the path to root
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Paths getPathToRoot(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the path to parent.
   *
   * @param conceptCode the concept code
   * @param parentConceptCode the parent concept code
   * @param dbType the db type
   * @return the path to parent
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Paths getPathToParent(String conceptCode, String parentConceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the all graph names.
   *
   * @param dbType the db type
   * @return the all graph names
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<String> getAllGraphNames(String dbType)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the evs version info.
   *
   * @param dbType the db type
   * @return the evs version info
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public EvsVersionInfo getEvsVersionInfo(String dbType)
    throws JsonParseException, JsonMappingException, IOException;

  /**
   * Returns the evs concept by label properties.
   *
   * @param conceptCode the concept code
   * @param dbType the db type
   * @param properties the properties
   * @return the evs concept by label properties
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public EvsConcept getEvsConceptByLabelProperties(String conceptCode, String dbType,
    List<String> properties) throws JsonMappingException, JsonParseException, IOException;

  /**
   * Returns the configuration data.
   *
   * @param dbType the db type
   * @return the configuration data
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public ConfigData getConfigurationData(String dbType)
    throws JsonMappingException, JsonProcessingException, IOException;

}
