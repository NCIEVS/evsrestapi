
package gov.nih.nci.evs.api.service;

import java.io.IOException;

import org.springframework.web.client.HttpClientErrorException;

import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.SearchCriteria;

/**
 * Represents a service that performs a search against an elasticsearch
 * endpoint.
 */
public interface ElasticSearchService {

  /**
   * Search.
   *
   * @param searchCriteria the search criteria
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws HttpClientErrorException the http client error exception
   * @throws Exception 
   */
  public ConceptResultList search(SearchCriteria searchCriteria)
    throws Exception;
}
