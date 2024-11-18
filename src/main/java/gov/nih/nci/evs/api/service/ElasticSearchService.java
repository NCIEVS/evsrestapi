package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.ConceptMap;
import gov.nih.nci.evs.api.model.ConceptMapResultList;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import org.springframework.web.client.HttpClientErrorException;

/** The Interface ElasticSearchService. */
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

  /**
   * search for the given search criteria in mappings.
   *
   * @param code the mapping code
   * @param searchCriteria the search criteria
   * @return the result list with concepts
   */
  ConceptMapResultList search(String code, SearchCriteria searchCriteria);

  /**
   * Returns the term query for mappings based on concepts.
   *
   * @param asList the as list
   * @param terminology the terminology
   * @return the mappings
   */
  public List<ConceptMap> getConceptMappings(List<String> asList, String terminology);
}
