package gov.nih.nci.evs.api.query;

/**
 * Matches concepts that another concept points to with a role of the given type, optionally from
 * the given source, e.g. concepts that are the primary anatomic site of Melanoma. A {@link
 * HasRelationship} on the concept's {@code inverseRoles}: see it for how matching works.
 *
 * <p>An inverse role is a role seen from the concept it points to: its type is the role's type, and
 * its target ({@code relatedCode}, {@code relatedName}) is the concept holding the role. So Skin's
 * inverse roles include "primary site: Melanoma" when Melanoma's roles include "primary site:
 * Skin".
 */
public final class HasInverseRole extends HasRelationship {

  /** The concept document's list of inverse roles, mapped as nested so they can be searched. */
  private static final String INVERSE_ROLES_FIELD_NAME = "inverseRoles";

  /** Instantiates an empty {@link HasInverseRole}. */
  public HasInverseRole() {
    // n/a
  }

  /**
   * Instantiates a {@link HasInverseRole} that matches an inverse role of the given type, from any
   * concept.
   *
   * @param role the role's type name or code
   */
  public HasInverseRole(final String role) {
    super(role);
  }

  /**
   * Instantiates a {@link HasInverseRole} that matches an inverse role of the given type from the
   * source.
   *
   * @param role the role's type name or code
   * @param source how the name or code of the concept holding the role that points to the matched
   *     concept
   */
  public HasInverseRole(final String role, final String source) {
    super(role, source);
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
    return INVERSE_ROLES_FIELD_NAME;
  }
}
