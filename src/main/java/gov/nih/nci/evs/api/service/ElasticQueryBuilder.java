
package gov.nih.nci.evs.api.service;

import java.io.IOException;

import gov.nih.nci.evs.api.model.SearchCriteria;

/**
 * Represents a builder for elastic search queries.
 */
public interface ElasticQueryBuilder {

  /**
   * Construct query.
   *
   * @param searchCriteria the search criteria
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public String constructQuery(SearchCriteria searchCriteria)
    throws IOException;

}
