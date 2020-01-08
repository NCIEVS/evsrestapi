
package gov.nih.nci.evs.api.model;

import gov.nih.nci.evs.api.support.SearchCriteria;

/**
 * Represents a list of results with paging parameters.
 */
public class ResultList extends BaseModel {

  /** The total. */
  private Integer total;

  /** The parameters. */
  private SearchCriteria parameters;

  /**
   * Instantiates an empty {@link ResultList}.
   */
  public ResultList() {
    // n/a
  }

  /**
   * Instantiates a {@link ResultList} from the specified parameters.
   *
   * @param other the other
   */
  public ResultList(final ResultList other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final ResultList other) {
    total = other.getTotal();
    parameters = other.getParameters();
  }

  /**
   * Returns the total.
   *
   * @return the total
   */
  public Integer getTotal() {
    return total;
  }

  /**
   * Sets the total.
   *
   * @param total the total
   */
  public void setTotal(final Integer total) {
    this.total = total;
  }


  /**
   * Returns the parameters.
   *
   * @return the parameters
   */
  public SearchCriteria getParameters() {
    return parameters;
  }

  /**
   * Sets the parameters.
   *
   * @param parameters the parameters
   */
  public void setParameters(final SearchCriteria parameters) {
    this.parameters = parameters;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((parameters == null) ? 0 : parameters.hashCode());
    result = prime * result + ((total == null) ? 0 : total.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ResultList other = (ResultList) obj;
    if (parameters == null) {
      if (other.parameters != null)
        return false;
    } else if (!parameters.equals(other.parameters))
      return false;
    if (total == null) {
      if (other.total != null)
        return false;
    } else if (!total.equals(other.total))
      return false;
    return true;
  }

}