package gov.nih.nci.evs.api.query;

/**
 * Matches concepts that another concept's association of the given type points to, optionally from
 * the given source: {@link HasAssociation} in the other direction. E.g. the subsets Cutaneous
 * Melanoma is in, which are the concepts its Concept_In_Subset associations point to.
 *
 * <p>A {@link HasRelationship} on the concept's {@code inverseAssociations}: see it for how
 * matching works. Each entry there mirrors another concept's association, with the same type, and
 * with that other concept as the entry's related concept. So the target here is the association's
 * source: the concept holding the association, not the one it points to.
 */
public final class HasInverseAssociation extends HasRelationship {

  /**
   * The concept document's list of inverse associations, mapped as nested so they can be searched.
   */
  private static final String INVERSE_ASSOCIATIONS_FIELD_NAME = "inverseAssociations";

  /** Instantiates an empty {@link HasInverseAssociation}. */
  public HasInverseAssociation() {
    // n/a
  }

  /**
   * Instantiates a {@link HasInverseAssociation} that matches concepts an association of the given
   * type points to, from any concept.
   *
   * @param association the association's type name or code
   */
  public HasInverseAssociation(final String association) {
    super(association);
  }

  /**
   * Instantiates a {@link HasInverseAssociation} that matches concepts an association of the given
   * type points to, from the given source.
   *
   * @param association the association's type name or code
   * @param source how the name or code of the concept the association must come from
   */
  public HasInverseAssociation(final String association, final String source) {
    super(association, source);
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
    return INVERSE_ASSOCIATIONS_FIELD_NAME;
  }
}
