
package gov.nih.nci.evs.api.model;

import gov.nih.nci.evs.api.model.evs.EvsAssociation;

/**
 * Represents a role relationship between two concepts.
 */
public class Role extends Relationship {
  /**
   * Instantiates an empty {@link Role}.
   */
  public Role() {
    // n/a
  }

  /**
   * Instantiates a {@link Role} from the specified parameters.
   *
   * @param other the other
   */
  public Role(final Role other) {
    populateFrom(other);
  }

  /**
   * Instantiates a {@link Role} from the specified parameters.
   *
   * @param other the other
   */
  public Role(final EvsAssociation other) {
    setType(other.getRelationship());
    setRelatedCode(other.getRelatedConceptCode());
    setRelatedName(other.getRelatedConceptLabel());
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Role other) {
    super.populateFrom(other);
  }

}
