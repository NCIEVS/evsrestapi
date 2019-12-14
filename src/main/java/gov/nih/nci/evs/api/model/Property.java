
package gov.nih.nci.evs.api.model;

/**
 * Represents a synonym of a concept.
 */
public class Property extends BaseModel {

  /** The type. */
  private String type;

  /** The value. */
  private String value;

  /**
   * Instantiates an empty {@link Property}.
   */
  public Property() {
    // n/a
  }

  /**
   * Instantiates a {@link Property} from the specified parameters.
   *
   * @param other the other
   */
  public Property(final Property other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Property other) {
    type = other.getType();
    value = other.getValue();
  }

  /**
   * Returns the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type the type
   */
  public void setType(final String type) {
    this.type = type;
  }

  /**
   * Returns the value.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value.
   *
   * @param value the value
   */
  public void setValue(final String value) {
    this.value = value;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Property other = (Property) obj;
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    if (value == null) {
      if (other.value != null) {
        return false;
      }
    } else if (!value.equals(other.value)) {
      return false;
    }
    return true;
  }

}
