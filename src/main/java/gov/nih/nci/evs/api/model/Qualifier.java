package gov.nih.nci.evs.api.model;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a qualifier on a synonym, definition, property, or role that isn't explicitly modeled
 * as a first-class attribute.
 */
@Schema(description = "Represents a type/value qualifier on a concept element")
@JsonIgnoreProperties(value = {"code"})
@JsonInclude(Include.NON_EMPTY)
public class Qualifier extends BaseModel implements Comparable<Qualifier> {

  /** The code. */
  // In the future we can use @WriteOnlyProperty
  // this does not work: @JsonProperty(access = Access.READ_ONLY)
  @Field(type = FieldType.Object, enabled = false)
  private String code;

  /** The type. */
  @Field(type = FieldType.Object, enabled = false)
  private String type;

  /** The value. */
  @Field(type = FieldType.Object, enabled = false)
  private String value;

  /** Instantiates an empty {@link Qualifier}. */
  public Qualifier() {
    // n/a
  }

  /**
   * Instantiates a {@link Qualifier} from the specified parameters.
   *
   * @param type the type
   * @param value the value
   */
  public Qualifier(final String type, final String value) {
    this.type = type;
    this.value = value;
  }

  /**
   * Instantiates a {@link Qualifier} from the specified parameters.
   *
   * @param other the other
   */
  public Qualifier(final Qualifier other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Qualifier other) {
    super.populateFrom(other);
    code = other.getCode();
    type = other.getType();
    value = other.getValue();
  }

  /**
   * Returns the code.
   *
   * @return the code
   */
  @Schema(hidden = true)
  public String getCode() {
    return code;
  }

  /**
   * Sets the code.
   *
   * @param code the code
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * Returns the type.
   *
   * @return the type
   */
  @Schema(description = "Qualifier type")
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
  @Schema(description = "Qualifier value")
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
    final Qualifier other = (Qualifier) obj;
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

  /* see superclass */
  @Override
  public int compareTo(Qualifier o) {
    return (type + value).compareToIgnoreCase(o.getType() + o.getValue());
  }
}
