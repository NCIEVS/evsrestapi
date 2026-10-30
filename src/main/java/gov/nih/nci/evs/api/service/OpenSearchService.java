package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.MappingResultList;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.query.Typeahead;
import java.util.List;
import org.springframework.web.client.HttpClientErrorException;

/** The Interface OpenSearchService. */
public interface OpenSearchService {

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
   * Runs a typeahead query against the concept indexes of its terminologies, returning matches in
   * the order the query ranks them.
   *
   * @param typeahead the typeahead query, which decides what matches and how matches are ranked
   * @param searchCriteria the search criteria supplying the page size and included fields, echoed
   *     back as the result parameters
   * @return the first page of matching concepts, best match first. The total counts matches only up
   *     to OpenSearch's default limit of 10,000, then reports 10,000. Unlike {@link #findConcepts},
   *     it doesn't run a second query for the exact count, since typeahead doesn't page.
   */
  public ConceptResultList findTypeaheadConcepts(
      Typeahead typeahead, SearchCriteria searchCriteria);

  /**
   * search for the given search criteria in mappings.
   *
   * @param query the query to search for the mappings
   * @param searchCriteria the search criteria
   * @return the result list with concepts
   */
  MappingResultList findConceptMappings(String query, SearchCriteria searchCriteria);

  /**
   * Returns the term query for mappings based on concepts.
   *
   * @param asList the concept codes
   * @param terminology the terminology
   * @return the mappings
   */
  public List<Mapping> getConceptMappings(List<String> asList, String terminology);
}
