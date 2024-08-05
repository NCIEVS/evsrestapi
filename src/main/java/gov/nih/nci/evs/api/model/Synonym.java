package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.DynamicMapping;
import org.springframework.data.elasticsearch.annotations.DynamicMappingValue;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/** Represents a synonym of a concept. */
@Schema(description = "Represents one of the (potentially many) names for a concept")
@JsonInclude(Include.NON_EMPTY)
public class Synonym extends BaseModel implements Comparable<Synonym> {

  /** The name. */
  @Field(type = FieldType.Text)
  private String name;

  /** The norm name. */
  // In the future we can use @WriteOnlyProperty
  // this does not work: @JsonProperty(access = Access.READ_ONLY)
  @Field(type = FieldType.Keyword)
  private String normName;

  /** The stemName. */
  // In the future we can use @WriteOnlyProperty
  // this does not work: @JsonProperty(access = Access.READ_ONLY)
  @Field(type = FieldType.Text)
  private String stemName;

  /** The highlight. */
  @Transient @JsonSerialize @JsonDeserialize private String highlight;

  /** The term type. */
  @Field(type = FieldType.Keyword)
  private String termType;

  /** The type. */
  @Field(type = FieldType.Keyword)
  private String type;

  /** The "code" of the synonym type, so it can be searched by. */
  // In the future we can use @WriteOnlyProperty
  // this does not work: @JsonProperty(access = Access.READ_ONLY)
  @Field(type = FieldType.Keyword)
  private String typeCode;

  /** The source. */
  @Field(type = FieldType.Keyword)
  private String source;

  /** The code. */
  @Field(type = FieldType.Keyword)
  private String code;

  /** The sub source. */
  @Field(type = FieldType.Keyword)
  private String subSource;

  /** The qualifiers - not NCIT, but could be other terminologies. */
  @Field(type = FieldType.Object, enabled = false)
  @DynamicMapping(DynamicMappingValue.False)
  private List<Qualifier> qualifiers;

  /** The active flag. */
  @Field(type = FieldType.Boolean)
  private Boolean active;

  /** Instantiates an empty {@link Synonym}. */
  public Synonym() {
    // n/a
  }

  /**
   * Instantiates a {@link Synonym} from the specified parameters.
   *
   * @param other the other
   */
  public Synonym(final Synonym other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Synonym other) {
    super.populateFrom(other);
    name = other.getName();
    highlight = other.getHighlight();
    termType = other.getTermType();
    type = other.getType();
    typeCode = other.getTypeCode();
    normName = other.getNormName();
    stemName = other.getStemName();
    source = other.getSource();
    code = other.getCode();
    subSource = other.getSubSource();
    qualifiers = new ArrayList<>(other.getQualifiers());
    active = other.isActive();
  }

  /**
   * Returns the name.
   *
   * @return the name
   */
  @Schema(description = "Name for a concept")
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the name
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Returns the normName.
   *
   * @return the normName
   */
  @Schema(hidden = true)
  public String getNormName() {
    return normName;
  }

  /**
   * Sets the normName.
   *
   * @param normName the normName
   */
  public void setNormName(String normName) {
    this.normName = normName;
  }

  /**
   * Returns the stem name.
   *
   * @return the stemName
   */
  @Schema(hidden = true)
  public String getStemName() {
    return stemName;
  }

  /**
   * Sets the stem name.
   *
   * @param stemName the stemName to set
   */
  public void setStemName(String stemName) {
    this.stemName = stemName;
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
   * Returns the term type.
   *
   * @return the term type
   */
  @Schema(description = "Synonym term type")
  public String getTermType() {
    return termType;
  }

  /**
   * Sets the term type.
   *
   * @param termType the term type
   */
  public void setTermType(final String termType) {
    this.termType = termType;
  }

  /**
   * Returns the type.
   *
   * @return the type
   */
  @Schema(description = "Synonym type")
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
   * Returns the type code.
   *
   * @return the type code
   */
  @Schema(hidden = true)
  public String getTypeCode() {
    return typeCode;
  }

  /**
   * Sets the type code.
   *
   * @param typeCode the type code
   */
  public void setTypeCode(final String typeCode) {
    this.typeCode = typeCode;
  }

  /**
   * Returns the source.
   *
   * @return the source
   */
  @Schema(description = "Synonym source")
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
   * Returns the code.
   *
   * @return the code
   */
  @Schema(
      description =
          "Code of the synonym, used in particular for "
              + "Metathesaurus data where the source of the synonym is not the terminology itself")
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
   * Returns the sub source.
   *
   * @return the sub source
   */
  @Schema(description = "Synonym sub-source")
  public String getSubSource() {
    return subSource;
  }

  /**
   * Sets the sub source.
   *
   * @param subSource the sub source
   */
  public void setSubSource(final String subSource) {
    this.subSource = subSource;
  }

  /**
   * Returns the qualifiers.
   *
   * @return the qualifiers
   */
  @Schema(description = "Type/value qualifiers on the synonym")
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
   * Returns the active flag.
   *
   * @return the active flag
   */
  @Schema(description = "Indicates whether the synonym is active")
  public Boolean isActive() {
    return active;
  }

  /**
   * Sets the active flag.
   *
   * @param active the active flag
   */
  public void setActive(final Boolean active) {
    this.active = active;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((code == null) ? 0 : code.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((source == null) ? 0 : source.hashCode());
    result = prime * result + ((subSource == null) ? 0 : subSource.hashCode());
    result = prime * result + ((termType == null) ? 0 : termType.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((active == null) ? 0 : active.hashCode());
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
    final Synonym other = (Synonym) obj;
    if (code == null) {
      if (other.code != null) {
        return false;
      }
    } else if (!code.equals(other.code)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (source == null) {
      if (other.source != null) {
        return false;
      }
    } else if (!source.equals(other.source)) {
      return false;
    }
    if (subSource == null) {
      if (other.subSource != null) {
        return false;
      }
    } else if (!subSource.equals(other.subSource)) {
      return false;
    }
    if (termType == null) {
      if (other.termType != null) {
        return false;
      }
    } else if (!termType.equals(other.termType)) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    if (active == null) {
      if (other.active != null) {
        return false;
      }
    } else if (!active.equals(other.active)) {
      return false;
    }
    return true;
  }

  /**
   * Compare to.
   *
   * @param other the other
   * @return the int
   */
  @Override
  public int compareTo(Synonym other) {
    return (source + type + name)
        .compareToIgnoreCase(other.getSource() + other.getType() + other.getName());
  }

  /** Clear hidden. */
  public void clearHidden() {
    normName = null;
    stemName = null;
    typeCode = null;
    getQualifiers().forEach(q -> q.clearHidden());
  }
}
