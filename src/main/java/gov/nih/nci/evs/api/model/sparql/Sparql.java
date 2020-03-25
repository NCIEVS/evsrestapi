
package gov.nih.nci.evs.api.model.sparql;

/**
 * The Class Sparql.
 */
public class Sparql {

  /** The results. */
  private Results results;

  /**
   * Returns the results.
   *
   * @return the results
   */
  public Results getResults() {
    return results;
  }

  /**
   * Sets the results.
   *
   * @param results the results
   */
  public void setResults(Results results) {
    this.results = results;
  }

  /**
   * To string.
   *
   * @return the string
   */
  @Override
  public String toString() {
    return "Sparql [results = " + results + "]";
  }
}