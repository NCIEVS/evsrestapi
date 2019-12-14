
package gov.nih.nci.evs.api.model;

/**
 * Represents a synonym of a concept.
 */
public class Definition extends BaseModel {

  /** The definition. */
  private String definition;

  /** The type. */
  private String type;

  /** The source. */
  private String source;

  /**
   * Instantiates an empty {@link Definition}.
   */
  public Definition() {
    // n/a
  }

  /**
   * Instantiates a {@link Definition} from the specified parameters.
   *
   * @param other the other
   */
  public Definition(final Definition other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Definition other) {
    definition = other.getDefinition();
    type = other.getType();
    source = other.getSource();
  }

  /**
   * Returns the definition.
   *
   * @return the definition
   */
  public String getDefinition() {
    return definition;
  }

  /**
   * Sets the definition.
   *
   * @param definition the definition
   */
  public void setDefinition(final String definition) {
    this.definition = definition;
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
   * Returns the source.
   *
   * @return the source
   */
  public String getSource() {
    return source;
  }

  /**
   * Sets the source.
   *
   * @param source the source
   */
  public void setSource(final String source) {
    this.source = source;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((definition == null) ? 0 : definition.hashCode());
    result = prime * result + ((source == null) ? 0 : source.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
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
    final Definition other = (Definition) obj;
    if (definition == null) {
      if (other.definition != null) {
        return false;
      }
    } else if (!definition.equals(other.definition)) {
      return false;
    }
    if (source == null) {
      if (other.source != null) {
        return false;
      }
    } else if (!source.equals(other.source)) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    return true;
  }

}
