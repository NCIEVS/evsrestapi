package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import org.springframework.web.client.HttpClientErrorException;

/** Represents a service that performs a search against an elasticsearch endpoint. */
public interface ElasticSearchService {

  /**
   * Search.
   *
   * @param terminologies the terminologies
   * @param searchCriteria the search criteria
   * @return the string
   * @throws Exception the exception
   * @throws HttpClientErrorException the http client error exception
   */
  public ConceptResultList search(List<Terminology> terminologies, SearchCriteria searchCriteria)
      throws Exception;
}
