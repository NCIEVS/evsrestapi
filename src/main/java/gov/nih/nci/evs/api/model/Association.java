
package gov.nih.nci.evs.api.model;

import gov.nih.nci.evs.api.model.evs.EvsAssociation;

/**
 * Represents an association between two concepts.
 */
public class Association extends Relationship {
  /**
   * Instantiates an empty {@link Association}.
   */
  public Association() {
    // n/a
  }

  /**
   * Instantiates a {@link Association} from the specified parameters.
   *
   * @param other the other
   */
  public Association(final Association other) {
    populateFrom(other);
  }

  /**
   * Instantiates a {@link Association} from the specified parameters.
   *
   * @param other the other
   */
  public Association(final EvsAssociation other) {
    setType(other.getRelationship());
    setRelatedCode(other.getRelatedConceptCode());
    setRelatedName(other.getRelatedConceptLabel());
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Association other) {
    super.populateFrom(other);
  }

}
