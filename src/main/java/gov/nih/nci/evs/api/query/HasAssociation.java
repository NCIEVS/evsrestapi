package gov.nih.nci.evs.api.query;

/**
 * Matches concepts that have an association of the given type, optionally pointing to the given
 * target, e.g. concepts in a subset of the CTRP Terminology. A {@link HasRelationship} on the
 * concept's {@code associations}: see it for how matching works.
 */
public final class HasAssociation extends HasRelationship {

  /** The concept document's list of associations, mapped as nested so they can be searched. */
  private static final String ASSOCIATIONS_FIELD_NAME = "associations";

  /** Instantiates an empty {@link HasAssociation}. */
  public HasAssociation() {
    // n/a
  }

  /**
   * Instantiates a {@link HasAssociation} that matches an association of the given type, pointing
   * to any concept.
   *
   * @param association the association's type name or code
   */
  public HasAssociation(final String association) {
    super(association);
  }

  /**
   * Instantiates a {@link HasAssociation} that matches an association of the given type pointing to
   * the target.
   *
   * @param association the association's type name or code
   * @param target how the name or code of the concept the association must point to
   */
  public HasAssociation(final String association, final String target) {
    super(association, target);
  }

  /**
   * Returns the name or code of the association's type, e.g. Concept_In_Subset (A8).
   *
   * @return the association name or code
   */
  public String getAssociation() {
    return getRelationshipType();
  }

  /**
   * Sets the name or code of the association's type, e.g. Concept_In_Subset (A8).
   *
   * @param association the association name or code
   */
  public void setAssociation(final String association) {
    setRelationshipType(association);
  }

  /* see superclass */
  @Override
  protected String getRelationshipListFieldName() {
    return ASSOCIATIONS_FIELD_NAME;
  }
}
