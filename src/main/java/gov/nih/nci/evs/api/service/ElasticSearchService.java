
package gov.nih.nci.evs.api.service;

import java.io.IOException;

import org.springframework.web.client.HttpClientErrorException;

import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.support.SearchCriteria;

/**
 * Represents a service that performs a search against an elasticsearch
 * endpoint.
 */
public interface ElasticSearchService {

  /**
   * Inits the settings.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws HttpClientErrorException the http client error exception
   */
  public void initSettings() throws IOException, HttpClientErrorException;

  /**
   * Search.
   *
   * @param searchCriteria the search criteria
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws HttpClientErrorException the http client error exception
   */
  public ConceptResultList search(SearchCriteria searchCriteria)
    throws IOException, HttpClientErrorException;

}
