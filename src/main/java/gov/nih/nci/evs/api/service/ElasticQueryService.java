package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.HierarchyUtils;

public interface ElasticQueryService {
  //TODO: comments
  Optional<Concept> getConcept(String code, Terminology terminology, IncludeParam ip);
  List<Concept> getConcepts(List<String> codes, Terminology terminology, IncludeParam ip);
  
  List<ConceptMinimal> getContributingSources(Terminology terminology) throws ClassNotFoundException, IOException;
  List<ConceptMinimal> getSynonymSources(Terminology terminology) throws ClassNotFoundException, IOException;
  List<Concept> getRoles(Terminology terminology);
  List<Concept> getAssociations(Terminology terminology);
  List<Concept> getProperties(Terminology terminology);
  List<Concept> getQualifiers(Terminology terminology);
  HierarchyUtils getHierarchy(Terminology terminology);
}
