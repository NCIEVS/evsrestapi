
package gov.nih.nci.evs.api.model;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a list of results with paging parameters.
 */
@Schema(description = "Represents a list of objects returned from a find call")
public class MapResultList extends ResultList {

  /** The total. */
  private List<Map<String, String>> results;

  /**
   * Instantiates an empty {@link MapResultList}.
   */
  public MapResultList() {
    // n/a
  }

  /**
   * Instantiates a {@link MapResultList} from the specified parameters.
   *
   * @param other the other
   */
  public MapResultList(final MapResultList other) {
    super.populateFrom(other);
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final MapResultList other) {
    super.populateFrom(other);
    results = other.getResults();
  }

  /**
   * Returns the results.
   *
   * @return the results
   */
  @Schema(description = "Search criteria used to arrive at this result")
  public List<Map<String, String>> getResults() {
    return results;
  }

  /**
   * Sets the results.
   *
   * @param results the results
   */
  public void setResults(final List<Map<String, String>> results) {
    this.results = results;
  }

  /**
   * Hash code.
   *
   * @return the int
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((results == null) ? 0 : results.hashCode());
    return result;
  }

  /**
   * Equals.
   *
   * @param obj the obj
   * @return true, if successful
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MapResultList other = (MapResultList) obj;
    if (results == null) {
      if (other.results != null)
        return false;
    }
    return true;
  }

}