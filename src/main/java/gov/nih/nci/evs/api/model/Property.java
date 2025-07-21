package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;

/** Represents a synonym of a concept. */
@Schema(description = "Represents a type/value property on a concept")
// @JsonIgnoreProperties(value = {
// "code"
// })
@JsonInclude(Include.NON_EMPTY)
public class Property extends BaseModel implements Comparable<Property> {

  /** The code. */
  // In the future we can use @WriteOnlyProperty
  // this does not work: @JsonProperty(access = Access.READ_ONLY)
  // @WriteOnlyProperty
  @Field(type = FieldType.Keyword)
  private String code;

  /** The type. */
  @Field(type = FieldType.Keyword)
  private String type;

  /** The value. */
  @Field(type = FieldType.Keyword)
  private String value;

  /** The highlight. */
  @Transient @JsonSerialize @JsonDeserialize private String highlight;

  /** The qualifiers. */
  @Field(type = FieldType.Object, enabled = false)
  @Mapping(enabled = false)
  private List<Qualifier> qualifiers;

  /** The source. */
  @Field(type = FieldType.Keyword)
  private String source;

  /** Instantiates an empty {@link Property}. */
  public Property() {
    // n/a
  }

  /**
   * Instantiates a {@link Property} from the specified parameters.
   *
   * @param type the type
   * @param value the value
   */
  public Property(final String type, final String value) {
    this.type = type;
    this.value = value;
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
    super.populateFrom(other);
    code = other.getCode();
    type = other.getType();
    value = other.getValue();
    highlight = other.getHighlight();
    qualifiers = new ArrayList<>(other.getQualifiers());
    source = other.getSource();
  }

  /**
   * Returns the code. This is really for internal use for connecting qualifiers to properties.
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
  @Schema(description = "Property type")
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
  @Schema(description = "Property value")
  public String getValue() {
    return value;
  }

  /**
   * Sets the value.
   *
   * @param value the value
   */
  public void setValue(final String value) {
    // There's really a max value limit in elasticsearch
    // If we've got something that's > 30k, then just chop to 30k
    if (value.length() > 30000) {
      this.value = value.substring(0, 30000);
    } else {
      this.value = value;
    }
  }

  /**
   * Returns the highlight.
   *
   * @return the highlight
   */
  @Schema(
      description =
          "Used by search calls to provide information for highlighting a view of results")
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
  @Schema(description = "Type/value qualifiers on the property")
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
   * Returns the source.
   *
   * @return the source
   */
  @Schema(description = "Property source")
  public String getSource() {
    return source;
  }

  /**
   * Sets the source.
   *
   * @param source the source
   */
  public void setSource(String source) {
    this.source = source;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    result = prime * result + ((source == null) ? 0 : source.hashCode());
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
    if (source == null) {
      if (other.source != null) {
        return false;
      }
    } else if (!source.equals(other.source)) {
      return false;
    }
    return true;
  }

  /* see superclass */
  @Override
  public int compareTo(Property o) {
    return (type + value).compareToIgnoreCase(o.getType() + o.getValue());
  }

  /** Clear hidden. */
  public void clearHidden() {
    code = null;
    getQualifiers().forEach(q -> q.clearHidden());
  }
}
