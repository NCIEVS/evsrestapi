
package gov.nih.nci.evs.api.model.sparql;

/**
 * The Class Sparql.
 */
public class Sparql {

  /** The results. */
  private Results results;

  /** The head. */
  private Head head;

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
   * @return the head
   */
  public Head getHead() {
    return head;
  }

  /**
   * @param head the head to set
   */
  public void setHead(Head head) {
    this.head = head;
  }

  /**
   * To string.
   *
   * @return the string
   */
  @Override
  public String toString() {
    return "Sparql [results = " + results + ", head = " + head + " ]";
  }
}