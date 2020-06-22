package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.HierarchyUtils;

public interface ElasticQueryService {
  //TODO: comments
  boolean checkConceptExists(String code, Terminology terminology);
  Optional<Concept> getConcept(String code, Terminology terminology, IncludeParam ip);
  List<Concept> getConcepts(List<String> codes, Terminology terminology, IncludeParam ip);
  List<HierarchyNode> getChildNodes(String parent, Terminology terminology)
      throws JsonParseException, JsonMappingException, IOException;
  List<HierarchyNode> getChildNodes(String parent, int maxLevel, Terminology terminology) 
      throws JsonParseException, JsonMappingException, IOException;  
  List<HierarchyNode> getRootNodes(Terminology terminology)
      throws JsonParseException, JsonMappingException, IOException;
  Optional<HierarchyUtils> getHierarchy(Terminology terminology) throws JsonMappingException, JsonProcessingException;
  List<Concept> getQualifiers(Terminology terminology) throws JsonMappingException, JsonProcessingException;
  List<Concept> getProperties(Terminology terminology) throws JsonMappingException, JsonProcessingException;
  List<Concept> getAssociations(Terminology terminology) throws JsonMappingException, JsonProcessingException;
  List<Concept> getRoles(Terminology terminology) throws JsonMappingException, JsonProcessingException;
  List<ConceptMinimal> getSynonymSources(Terminology terminology) throws ClassNotFoundException, IOException;  
  List<ConceptMinimal> getContributingSources(Terminology terminology) throws ClassNotFoundException, IOException;
}
