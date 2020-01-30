
package gov.nih.nci.evs.api.service;

import java.io.IOException;

import gov.nih.nci.evs.api.support.FilterCriteriaElasticFields;
import gov.nih.nci.evs.api.support.SearchCriteria;

/**
 * Represents a builder for elastic search queries.
 */
public interface ElasticQueryBuilder {

  /**
   * Construct query.
   *
   * @param filterCriteriaElasticFields the filter criteria elastic fields
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  String constructQuery(FilterCriteriaElasticFields filterCriteriaElasticFields)
    throws IOException;

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
