package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.ConceptMapResultList;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.Mappings;
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
  public ConceptResultList findConcepts(
      List<Terminology> terminologies, SearchCriteria searchCriteria) throws Exception;

  /**
   * search for the given search criteria in mappings.
   *
   * @param query the query to search for the mappings
   * @param searchCriteria the search criteria
   * @return the result list with concepts
   * @throws Exception the exception
   */
  ConceptMapResultList findConceptMappings(String query, SearchCriteria searchCriteria);

  /**
   * Returns the term query for mappings based on concepts.
   *
   * @param asList the concept codes
   * @param terminology the terminology
   * @return the mappings
   * @throws Exception the exception
   */
  public List<Mappings> getConceptMappings(List<String> asList, String terminology);
}
