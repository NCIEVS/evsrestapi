package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;

import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Terminology;

public interface ElasticObjectService {
  //TODO: comments
  List<ConceptMinimal> getContributingSources(Terminology terminology) throws ClassNotFoundException, IOException;
  List<ConceptMinimal> getSynonymSources(Terminology terminology) throws ClassNotFoundException, IOException;
}
