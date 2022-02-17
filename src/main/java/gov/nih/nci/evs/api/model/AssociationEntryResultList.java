
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a list of results with paging parameters.
 */
public class AssociationEntryResultList extends ResultList {

  /** The concepts. */
  private List<AssociationEntry> concepts;

  /**
   * Instantiates an empty {@link AssociationEntryResultList}.
   */
  public AssociationEntryResultList() {
    // n/a
  }

  /**
   * Instantiates a {@link AssociationEntryResultList} from the specified parameters.
   *
   * @param other the other
   */
  public AssociationEntryResultList(final AssociationEntryResultList other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final AssociationEntryResultList other) {
    super.populateFrom(other);
    concepts = new ArrayList<>(other.getAssociationEntries());
  }

  /**
   * Returns the concepts.
   *
   * @return the concepts
   */
  public List<AssociationEntry> getAssociationEntries() {
    if (concepts == null) {
      concepts = new ArrayList<>();
    }
    return concepts;
  }

  /**
   * Sets the concepts.
   *
   * @param concepts the concepts
   */
  public void setAssociationEntries(final List<AssociationEntry> concepts) {
    this.concepts = concepts;
  }

}