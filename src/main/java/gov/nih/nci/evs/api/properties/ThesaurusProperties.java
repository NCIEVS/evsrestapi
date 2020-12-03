
package gov.nih.nci.evs.api.properties;

import java.util.Map;

/**
 * The Class ThesaurusProperties.
 */
public class ThesaurusProperties {

  /** The roles. */
  private Map<String, String> roles;

  /** The associations. */
  private Map<String, String> associations;

  /** The sources. */
  private Map<String, String> sources;

  /** The term types. */
  private Map<String, String> termTypes;

  /** The return fields. */
  private Map<String, String> returnFields;

  /** The property not considered. */
  private Map<String, String> propertyNotConsidered;

  /** The return fields by label. */
  // code to be removed
  private Map<String, String> returnFieldsByLabel;

  /** The sources to be removed. */
  private String[] sourcesToBeRemoved;

  /**
   * Returns the roles.
   *
   * @return the roles
   */
  public Map<String, String> getRoles() {
    return roles;
  }

  /**
   * Sets the roles.
   *
   * @param roles the roles
   */
  public void setRoles(Map<String, String> roles) {
    this.roles = roles;
  }

  /**
   * Returns the associations.
   *
   * @return the associations
   */
  public Map<String, String> getAssociations() {
    return associations;
  }

  /**
   * Sets the associations.
   *
   * @param associations the associations
   */
  public void setAssociations(Map<String, String> associations) {
    this.associations = associations;
  }

  /**
   * Returns the return fields.
   *
   * @return the return fields
   */
  public Map<String, String> getReturnFields() {
    return returnFields;
  }

  /**
   * Sets the return fields.
   *
   * @param returnFields the return fields
   */
  public void setReturnFields(Map<String, String> returnFields) {
    this.returnFields = returnFields;
  }

  /**
   * Returns the return fields by label.
   *
   * @return the return fields by label
   */
  public Map<String, String> getReturnFieldsByLabel() {
    return returnFieldsByLabel;
  }

  /**
   * Sets the return fields by label.
   *
   * @param returnFieldsByLabel the return fields by label
   */
  public void setReturnFieldsByLabel(Map<String, String> returnFieldsByLabel) {
    this.returnFieldsByLabel = returnFieldsByLabel;
  }

  /**
   * Returns the sources.
   *
   * @return the sources
   */
  public Map<String, String> getSources() {
    return sources;
  }

  /**
   * Sets the sources.
   *
   * @param sources the sources
   */
  public void setsources(Map<String, String> sources) {
    this.sources = sources;
  }

  /**
   * Returns the term types.
   *
   * @return the term types
   */
  public Map<String, String> getTermTypes() {
    return termTypes;
  }

  /**
   * Sets the term types.
   *
   * @param termTypesSources the term types sources
   */
  public void setTermTypes(Map<String, String> termTypes) {
    this.termTypes = termTypes;
  }

  /**
   * Returns the property not considered.
   *
   * @return the property not considered
   */
  public Map<String, String> getPropertyNotConsidered() {
    return propertyNotConsidered;
  }

  /**
   * Sets the property not considered.
   *
   * @param propertyNotConsidered the property not considered
   */
  public void setPropertyNotConsidered(Map<String, String> propertyNotConsidered) {
    this.propertyNotConsidered = propertyNotConsidered;
  }

  /**
   * Returns the sources to be removed.
   *
   * @return the sources to be removed
   */
  public String[] getSourcesToBeRemoved() {
    return sourcesToBeRemoved;
  }

  /**
   * Sets the sources to be removed.
   *
   * @param sourcesToBeRemoved the sources to be removed
   */
  public void setSourcesToBeRemoved(String[] sourcesToBeRemoved) {
    this.sourcesToBeRemoved = sourcesToBeRemoved;
  }

}
