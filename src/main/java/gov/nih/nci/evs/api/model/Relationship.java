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

/** Represents a connection between two concepts. */
@Schema(description = "Represents a connection between two concepts")
@JsonInclude(Include.NON_EMPTY)
public class Relationship extends BaseModel implements Comparable<Relationship> {

  /** The code. */
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
  @Transient @JsonSerialize @JsonDeserialize private String highlight;

  /** The qualifiers. */
  @Field(type = FieldType.Object, enabled = false)
  @Mapping(enabled = false)
  private List<Qualifier> qualifiers;

  /** Instantiates an empty {@link Relationship}. */
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
    super.populateFrom(other);
    code = other.getCode();
    type = other.getType();
    relatedCode = other.getRelatedCode();
    relatedName = other.getRelatedName();
    source = other.getSource();
    highlight = other.getHighlight();
    qualifiers = new ArrayList<>(other.getQualifiers());
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
  @Schema(description = "Relationship type")
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
  @Schema(description = "Related code (the code on the other side of the relationship)")
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
  @Schema(description = "Preferred name of the related code")
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
  @Schema(description = "Relationship source")
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
   * Returns the qualifiers.
   *
   * @return the qualifiers
   */
  @Schema(description = "Type/value qualifiers on the relationship")
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

  /** Clear hidden. */
  public void clearHidden() {
    code = null;
    getQualifiers().forEach(q -> q.clearHidden());
  }
}
