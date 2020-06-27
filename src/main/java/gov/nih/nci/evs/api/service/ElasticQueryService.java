package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.HierarchyUtils;

/**
 * The elasticsearch DB query service
 * 
 * @author Arun
 *
 */
public interface ElasticQueryService {
  
  /**
   * Check if concept exists
   * 
   * @param code the code of the concept to check
   * @param terminology the terminology
   * @return boolean indicating if code exists or not
   */
  boolean checkConceptExists(String code, Terminology terminology);
  
  /**
   * Returns concept
   * 
   * @param code the code of the concept
   * @param terminology the terminology
   * @param ip the include param
   * @return the optional of concept for the code
   */
  Optional<Concept> getConcept(String code, Terminology terminology, IncludeParam ip);
  
  /**
   * Returns list of concepts
   * 
   * @param codes the list of concept codes
   * @param terminology the terminology
   * @param ip the include param
   * @return the list of concepts found
   */
  List<Concept> getConcepts(List<String> codes, Terminology terminology, IncludeParam ip);
  
  /**
   * Returns concepts mapped by code
   * 
   * @param codes the list of concept codes
   * @param terminology the terminology
   * @param ip the include param
   * @return concepts mapped by code
   */
  Map<String, Concept> getConceptsAsMap(List<String> codes, Terminology terminology, IncludeParam ip);
  
  /**
   * Returns child concepts
   * 
   * @param code the code of the parent concept
   * @param terminology the terminology
   * @return the list of child concepts
   */
  List<Concept> getSubclasses(String code, Terminology terminology);
  
  /**
   * Returns parent concepts
   * 
   * @param code the code of the child concept
   * @param terminology the terminology
   * @return the list of parent concepts
   */
  List<Concept> getSuperclasses(String code, Terminology terminology);
  
  /**
   * Returns the label of the concept
   * 
   * @param code the code of the concept
   * @param terminology the terminology
   * @return the optional of code label
   */
  Optional<String> getLabel(String code, Terminology terminology);
  
  /**
   * Returns the root nodes
   * 
   * @param terminology the terminology
   * @return the list of root nodes
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  List<HierarchyNode> getRootNodes(Terminology terminology) throws JsonParseException, JsonMappingException, IOException;
  
  /**
   * Returns the child nodes
   * 
   * @param parent the parent code
   * @param terminology the terminology
   * @return the list of child nodes
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  List<HierarchyNode> getChildNodes(String parent, Terminology terminology) throws JsonParseException, JsonMappingException, IOException;
  
  /**
   * Returns the child nodes
   * 
   * @param parent the parent code
   * @param maxLevel the max level upto which child nodes to be fetched
   * @param terminology the terminology
   * @return the list of child nodes
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  List<HierarchyNode> getChildNodes(String parent, int maxLevel, Terminology terminology) throws JsonParseException, JsonMappingException, IOException;
  
  /**
   * Returns all child codes (obtained recursively)
   * 
   * @param code the parent code
   * @param terminology the terminology
   * @return the list of child codes
   */
  List<String> getAllChildNodes(String code, Terminology terminology);
  
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
  List<HierarchyNode> getPathInHierarchy(String code, Terminology terminology) throws JsonParseException, JsonMappingException, IOException;
  
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
  void checkPathInHierarchy(String code, HierarchyNode node, Path path, Terminology terminology) throws JsonParseException, JsonMappingException, IOException;
  
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
  Paths getPathToRoot(String code, Terminology terminology) throws JsonParseException, JsonMappingException, IOException;
  
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
  Paths getPathToParent(String code, String parentCode, Terminology terminology) throws JsonParseException, JsonMappingException, IOException;
  
  /**
   * Get hiearchy for a given terminology.
   *
   * @param terminology the terminology
   * @return the hierarchy
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Optional<HierarchyUtils> getHierarchy(Terminology terminology) throws JsonMappingException, JsonProcessingException;
  
  /**
   * Returns qualifiers
   * 
   * @param terminology the terminology
   * @return the list of qualifier concepts
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  List<Concept> getQualifiers(Terminology terminology) throws JsonMappingException, JsonProcessingException;
  
  /**
   * Returns properties
   * 
   * @param terminology the terminology
   * @return the list of property concepts
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  List<Concept> getProperties(Terminology terminology) throws JsonMappingException, JsonProcessingException;

  /**
   * Returns associations
   * 
   * @param terminology the terminology
   * @return the list of association concepts
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  List<Concept> getAssociations(Terminology terminology) throws JsonMappingException, JsonProcessingException;

  /**
   * Returns roles
   * 
   * @param terminology the terminology
   * @return the list of role concepts
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  List<Concept> getRoles(Terminology terminology) throws JsonMappingException, JsonProcessingException;
  
  /**
   * Returns synonym sources
   * 
   * @param terminology the terminology
   * @return the list of synonym source concepts
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  List<ConceptMinimal> getSynonymSources(Terminology terminology) throws ClassNotFoundException, IOException;  

  /**
   * Returns contributing sources
   * 
   * @param terminology the terminology
   * @return the list of contributing source concepts
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  List<ConceptMinimal> getContributingSources(Terminology terminology) throws ClassNotFoundException, IOException;
}
