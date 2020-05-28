package gov.nih.nci.evs.api.service;

import java.util.List;

import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Terminology;

public interface ElasticObjectService {
  //TODO: comments
  List<ConceptMinimal> getContributingSources(Terminology terminology);
}
