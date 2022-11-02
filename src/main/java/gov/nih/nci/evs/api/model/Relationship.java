
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
 * Represents a connection between two concepts.
 */
@JsonInclude(Include.NON_EMPTY)
public class Relationship extends BaseModel implements Comparable<Relationship> {

  /** The code. */
  @JsonProperty(access = Access.READ_ONLY)
  @Field(type = FieldType.Keyword)
  private String code;

  /** The type. */
  @Field(type = FieldType.Keyword)
  private String type;

  /** The related code. */
  @Field(type = FieldType.Keyword)
  private String relatedCode;

  /** The related name. */
  @Field(type = FieldType.Text)
  private String relatedName;

  /** The source. */
  @Field(type = FieldType.Keyword)
  private String source;

  /** The highlight. */
  @Transient
  @JsonSerialize
  @JsonDeserialize
  private String highlight;

  /** The qualifiers. */
  @Field(type = FieldType.Nested)
  private List<Qualifier> qualifiers;

  /**
   * Instantiates an empty {@link Relationship}.
   */
  public Relationship() {
    // n/a
  }

  /**
   * Instantiates a {@link Relationship} from the specified parameters.
   *
   * @param other the other
   */
  public Relationship(final Relationship other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Relationship other) {
    code = other.getCode();
    type = other.getType();
    relatedCode = other.getRelatedCode();
    relatedName = other.getRelatedName();
    source = other.getSource();
    highlight = other.getHighlight();
    qualifiers = new ArrayList<>(other.getQualifiers());
  }

  /**
   * Returns the code. This is really for internal use for connecting qualifiers
   * to properties.
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
   * Returns the related code.
   *
   * @return the related code
   */
  public String getRelatedCode() {
    return relatedCode;
  }

  /**
   * Sets the related code.
   *
   * @param relatedCode the related code
   */
  public void setRelatedCode(final String relatedCode) {
    this.relatedCode = relatedCode;
  }

  /**
   * Returns the related name.
   *
   * @return the related name
   */
  public String getRelatedName() {
    return relatedName;
  }

  /**
   * Sets the related name.
   *
   * @param relatedName the related name
   */
  public void setRelatedName(final String relatedName) {
    this.relatedName = relatedName;
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

  /**
   * Returns the highlight.
   *
   * @return the highlight
   */
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

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((relatedCode == null) ? 0 : relatedCode.hashCode());
    result = prime * result + ((relatedName == null) ? 0 : relatedName.hashCode());
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
    final Relationship other = (Relationship) obj;
    if (relatedCode == null) {
      if (other.relatedCode != null) {
        return false;
      }
    } else if (!relatedCode.equals(other.relatedCode)) {
      return false;
    }
    if (relatedName == null) {
      if (other.relatedName != null) {
        return false;
      }
    } else if (!relatedName.equals(other.relatedName)) {
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
  public int compareTo(Relationship o) {
    return (relatedName + source + relatedCode + type)
        .compareToIgnoreCase(o.getRelatedName() + o.getSource() + o.getRelatedCode() + o.getType());
  }

}
