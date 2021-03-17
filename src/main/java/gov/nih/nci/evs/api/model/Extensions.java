
package gov.nih.nci.evs.api.model;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a synonym of a concept.
 */
// Show empty "mainMenuAncestors" rather than nothing.
@JsonInclude(Include.NON_NULL)
public class Extensions extends BaseModel implements Comparable<Extensions> {

  /** CTRP "is disease" flag. */
  @Field(type = FieldType.Boolean)
  private Boolean isDisease;

  /** CTRP "is subtype" flag. */
  @Field(type = FieldType.Boolean)
  private Boolean isSubtype;

  /** The paths to root. */
  @Field(type = FieldType.Object, ignoreFields = {
      "paths"
  })
  private List<Paths> mainMenuAncestors;

  /**
   * Instantiates an empty {@link Extensions}.
   */
  public Extensions() {
    // n/a
  }

  /**
   * Instantiates a {@link Extensions} from the specified parameters.
   *
   * @param other the other
   */
  public Extensions(final Extensions other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Extensions other) {
    isDisease = other.getIsDisease();
    isSubtype = other.getIsSubtype();
    mainMenuAncestors = other.getMainMenuAncestors();
  }

  /**
   * Returns the is disease.
   *
   * @return the is disease
   */
  public Boolean getIsDisease() {
    return isDisease;
  }

  /**
   * Sets the is disease.
   *
   * @param isDisease the is disease
   */
  public void setIsDisease(final Boolean isDisease) {
    this.isDisease = isDisease;
  }

  /**
   * Returns the is subtype.
   *
   * @return the is subtype
   */
  public Boolean getIsSubtype() {
    return isSubtype;
  }

  /**
   * Sets the is subtype.
   *
   * @param isSubtype the is subtype
   */
  public void setIsSubtype(final Boolean isSubtype) {
    this.isSubtype = isSubtype;
  }

  /**
   * Main menu ancestors.
   *
   * @return the paths
   */
  public List<Paths> getMainMenuAncestors() {
    return mainMenuAncestors;
  }

  /**
   * Sets the main menu ancestors.
   *
   * @param mainMenuAncestors the main menu ancestors
   */
  public void setMainMenuAncestors(final List<Paths> mainMenuAncestors) {
    this.mainMenuAncestors = mainMenuAncestors;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((isDisease == null) ? 0 : isDisease.hashCode());
    result = prime * result + ((isSubtype == null) ? 0 : isSubtype.hashCode());
    result = prime * result + ((mainMenuAncestors == null) ? 0 : mainMenuAncestors.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Extensions other = (Extensions) obj;
    if (isDisease == null) {
      if (other.isDisease != null)
        return false;
    } else if (!isDisease.equals(other.isDisease))
      return false;
    if (isSubtype == null) {
      if (other.isSubtype != null)
        return false;
    } else if (!isSubtype.equals(other.isSubtype))
      return false;
    if (mainMenuAncestors == null) {
      if (other.mainMenuAncestors != null)
        return false;
    } else if (!mainMenuAncestors.equals(other.mainMenuAncestors))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public int compareTo(Extensions o) {
    return (o + "").compareTo(o + "");
  }

}
