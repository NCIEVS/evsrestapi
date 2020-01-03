
package gov.nih.nci.evs.api.service;

import java.io.IOException;

import org.springframework.web.client.HttpClientErrorException;

import gov.nih.nci.evs.api.support.FilterCriteriaElasticFields;
import gov.nih.nci.evs.api.support.SearchCriteria;

/**
 * Represents a service that performs a search against an elasticsearch endpoint.
 */
public interface ElasticSearchService {

  /**
   * Elasticsearch.
   *
   * @param filterCriteriaElasticFields the filter criteria elastic fields
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws HttpClientErrorException the http client error exception
   */
  public String elasticsearch(
    FilterCriteriaElasticFields filterCriteriaElasticFields)
    throws IOException, HttpClientErrorException;

  /**
   * Search.
   *
   * @param searchCriteria the search criteria
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws HttpClientErrorException the http client error exception
   */
  public String search(SearchCriteria searchCriteria)
    throws IOException, HttpClientErrorException;

}
