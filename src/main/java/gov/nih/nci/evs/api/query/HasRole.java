package gov.nih.nci.evs.api.query;

/**
 * Matches concepts that have a role of the given type, optionally pointing to the given target,
 * e.g. concepts whose primary anatomic site is Skin. A {@link HasRelationship} on the concept's
 * {@code roles}: see it for how matching works.
 */
public final class HasRole extends HasRelationship {

  /** The concept document's list of roles, mapped as nested so roles can be searched. */
  private static final String ROLES_FIELD_NAME = "roles";

  /** Instantiates an empty {@link HasRole}. */
  public HasRole() {
    // n/a
  }

  /**
   * Instantiates a {@link HasRole} that matches a role of the given type, pointing to any concept.
   *
   * @param role the role's type name or code
   */
  public HasRole(final String role) {
    super(role);
  }

  /**
   * Instantiates a {@link HasRole} that matches a role of the given type pointing to the target.
   *
   * @param role the role's type name or code
   * @param target how the name or code of the concept the role must point to
   */
  public HasRole(final String role, final String target) {
    super(role, target);
  }

  /**
   * Returns the name or code of the role's type, e.g. Disease_Has_Primary_Anatomic_Site (R101).
   *
   * @return the role name or code
   */
  public String getRole() {
    return getRelationshipType();
  }

  /**
   * Sets the name or code of the role's type, e.g. Disease_Has_Primary_Anatomic_Site (R101).
   *
   * @param role the role name or code
   */
  public void setRole(final String role) {
    setRelationshipType(role);
  }

  /* see superclass */
  @Override
  protected String getRelationshipListFieldName() {
    return ROLES_FIELD_NAME;
  }
}
