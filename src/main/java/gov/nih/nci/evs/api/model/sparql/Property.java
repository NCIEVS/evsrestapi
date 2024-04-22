package gov.nih.nci.evs.api.model.sparql;

/** The Class Property. */
public class Property {

  /** The value. */
  private String value;

  /** The type. */
  private String type;

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
  public void setValue(String value) {
    this.value = value;
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
  public void setType(String type) {
    this.type = type;
  }

  /**
   * To string.
   *
   * @return the string
   */
  @Override
  public String toString() {
    return "[value = " + value + ", type = " + type + "]";
  }
}
