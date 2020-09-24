
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents a synonym of a concept.
 */
//@JsonIgnoreProperties(value = {
//    "code"
//})
@JsonInclude(Include.NON_EMPTY)
public class Property extends BaseModel {

  /** The code. */
  @JsonProperty(access = Access.READ_ONLY)
  @Field(type = FieldType.Keyword)
  private String code;

  /** The type. */
  @Field(type = FieldType.Keyword)
  private String type;

  /** The value. */
  @Field(type = FieldType.Keyword)
  private String value;

  /** The highlight. */
  @Transient
  @JsonSerialize
  @JsonDeserialize
  private String highlight;

  /** The qualifiers. */
  private List<Qualifier> qualifiers;

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
    code = other.getCode();
    type = other.getType();
    value = other.getValue();
    highlight = other.getHighlight();
    qualifiers = new ArrayList<>(other.getQualifiers());
  }

  /**
   * Returns the code.  This is really for internal use for connecting qualifiers to properties.
   *
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * Sets the code.
   *
   * @param code the code
   */
  public void setCode(final String code) {
    this.code = code;
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

  /**
   * Returns the highlight.
   *
   * @return the highlight
   */
  public String getHighlight() {
    return highlight;
  }

  /**
   * Sets the hightlight.
   *
   * @param highlight the hightlight
   */
  public void setHighlight(final String highlight) {
    this.highlight = highlight;
  }

  /**
   * Returns the qualifiers.
   *
   * @return the qualifiers
   */
  public List<Qualifier> getQualifiers() {
    if (qualifiers == null) {
      qualifiers = new ArrayList<>();
    }
    return qualifiers;
  }

  /**
   * Sets the qualifiers.
   *
   * @param qualifiers the qualifiers
   */
  public void setQualifiers(final List<Qualifier> qualifiers) {
    this.qualifiers = qualifiers;
  }

  /**
   * Hash code.
   *
   * @return the int
   */
  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  /**
   * Equals.
   *
   * @param obj the obj
   * @return true, if successful
   */
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
