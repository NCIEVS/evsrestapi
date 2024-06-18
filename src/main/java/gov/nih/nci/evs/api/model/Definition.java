package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.DynamicMapping;
import org.springframework.data.elasticsearch.annotations.DynamicMappingValue;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.v3.oas.annotations.media.Schema;

/** Represents a synonym of a concept. */
@Schema(description = "Represents a text definition for a concept")
@JsonInclude(Include.NON_EMPTY)
public class Definition extends BaseModel implements Comparable<Definition> {

  /** The definition. */
  @Field(type = FieldType.Text)
  private String definition;

  /** The highlight. */
  @Transient @JsonSerialize @JsonDeserialize private String highlight;

  /** The "code" of the definition type. */
  // In the future we can use @WriteOnlyProperty
  // this does not work: @JsonProperty(access = Access.READ_ONLY)
  @Field(type = FieldType.Keyword)
  private String code;

  /** The type. */
  @Field(type = FieldType.Keyword)
  private String type;

  /** The source. */
  @Field(type = FieldType.Keyword)
  private String source;

  /** The qualifiers. */
  @Field(type = FieldType.Object, enabled = false)
  @DynamicMapping(DynamicMappingValue.False)
  private List<Qualifier> qualifiers;

  /** Instantiates an empty {@link Definition}. */
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
    super.populateFrom(other);
    definition = other.getDefinition();
    highlight = other.getHighlight();
    code = other.getCode();
    type = other.getType();
    source = other.getSource();
    qualifiers = new ArrayList<>(other.getQualifiers());
  }

  /**
   * Returns the definition.
   *
   * @return the definition
   */
  @Schema(description = "Text definition value")
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
   * Sets the highlight.
   *
   * @param highlight the highlight
   */
  public void setHighlight(final String highlight) {
    this.highlight = highlight;
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
  public void setCode(final String code) {
    this.code = code;
  }

  /**
   * Returns the type.
   *
   * @return the type
   */
  @Schema(description = "Definition type")
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
  @Schema(description = "Definition source")
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

  /**
   * Returns the qualifiers.
   *
   * @return the qualifiers
   */
  @Schema(description = "Type/value qualifiers on the definition")
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

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((definition == null) ? 0 : definition.hashCode());
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

  /* see superclass */
  @Override
  public int compareTo(Definition o) {
    return (definition + "").compareTo(o.getDefinition() + "");
  }
}
