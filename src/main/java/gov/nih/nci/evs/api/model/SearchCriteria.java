package gov.nih.nci.evs.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

/** Search criteria object for /concept/search implementation. */
@Schema(description = "Criteria for a search or find operation")
public class SearchCriteria extends SearchCriteriaWithoutTerminology {

  /** The terminology. */
  private List<String> terminology;

  /** The sparql. */
  private String sparql;

  /** Instantiates an empty {@link SearchCriteria}. */
  public SearchCriteria() {
    // n/a
  }

  /**
   * Instantiates a {@link SearchCriteria} from the specified parameters.
   *
   * @param other the other
   */
  public SearchCriteria(final SearchCriteria other) {
    populateFrom(other);
    terminology = new ArrayList<>(other.getTerminology());
    sparql = other.getSparql();
  }

  /**
   * Instantiates a {@link SearchCriteria} from the specified parameters.
   *
   * @param other the other
   * @param terminology the terminology
   */
  public SearchCriteria(final SearchCriteriaWithoutTerminology other, final String terminology) {
    populateFrom(other);
    getTerminology().add(terminology);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  @Override
  public void populateFrom(final SearchCriteriaWithoutTerminology other) {
    super.populateFrom(other);
    if (other instanceof SearchCriteria) {
      terminology = new ArrayList<>(((SearchCriteria) other).getTerminology());
    }
  }

  /**
   * Returns the terminologies.
   *
   * @return the terminologies
   */
  @Schema(description = "Comma-separated list of terminologies to search")
  public List<String> getTerminology() {
    if (terminology == null) {
      terminology = new ArrayList<>();
    }
    return terminology;
  }

  /**
   * Sets the terminology.
   *
   * @param terminology the terminology
   */
  public void setTerminology(final List<String> terminology) {
    this.terminology = terminology;
  }

  /**
   * Returns the sparql.
   *
   * @return the sparql
   */
  @Schema(description = "SPARQL query, only available as an output parameter")
  public String getSparql() {
    return sparql;
  }

  /**
   * Sets the sparql.
   *
   * @param sparql the sparql
   */
  public void setSparql(final String sparql) {
    this.sparql = sparql;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SearchCriteria other = (SearchCriteria) obj;
    if (terminology == null) {
      if (other.terminology != null) {
        return false;
      }
    } else if (!terminology.equals(other.terminology)) {
      return false;
    }
    return true;
  }
}
