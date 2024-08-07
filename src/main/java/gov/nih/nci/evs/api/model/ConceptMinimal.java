package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Represents a concept with a code from a terminology with the smallest amount of information.
 *
 * <pre>
 * {
 *   "code" : "C3224",
 *   "name" : "Melanoma",
 * }
 * </pre>
 */
@Schema(description = "Represents minimal information about a concept in a terminology")
@JsonInclude(Include.NON_EMPTY)
public class ConceptMinimal extends BaseModel implements Comparable<ConceptMinimal> {

  /** The code. */
  @Id
  @Field(type = FieldType.Text)
  private String code;

  /** The uri. */
  @Field(type = FieldType.Keyword)
  private String uri;

  /** The name. */
  @Field(type = FieldType.Text)
  private String name;

  /** The terminology. */
  @Field(type = FieldType.Text)
  private String terminology;

  /** The version. */
  @Field(type = FieldType.Text)
  private String version;

  /** The level. */
  @Field(type = FieldType.Integer)
  private Integer level;

  /** Instantiates an empty {@link ConceptMinimal}. */
  public ConceptMinimal() {
    // n/a
  }

  /**
   * Instantiates a {@link ConceptMinimal} from the specified parameters.
   *
   * @param code the code
   */
  public ConceptMinimal(final String code) {
    this.code = code;
  }

  /**
   * Instantiates a {@link ConceptMinimal} from the specified parameters.
   *
   * @param terminology the terminology
   * @param code the code
   * @param name the name
   */
  public ConceptMinimal(final String terminology, final String code, final String name) {
    this.terminology = terminology;
    this.code = code;
    this.name = name;
  }

  /**
   * Instantiates a {@link ConceptMinimal} from the specified parameters.
   *
   * @param other the other
   */
  public ConceptMinimal(final ConceptMinimal other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final ConceptMinimal other) {
    super.populateFrom(other);
    uri = other.getUri();
    code = other.getCode();
    name = other.getName();
    terminology = other.getTerminology();
    version = other.getVersion();
    level = other.getLevel();
  }

  /* see superclass */
  @Override
  @Schema(description = "URI for this element in an rdf-based source file")
  public String getUri() {
    return uri;
  }

  /* see superclass */
  @Override
  public void setUri(final String uri) {
    this.uri = uri;
  }

  /**
   * Returns the code.
   *
   * @return the code
   */
  @Schema(description = "Code (unique identifier) for this meaning")
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
   * Returns the name.
   *
   * @return the name
   */
  @Schema(description = "Preferred name for the code")
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
   * Returns the terminology.
   *
   * @return the terminology
   */
  @Schema(description = "Terminology abbreviation, e.g. 'nci'")
  public String getTerminology() {
    return terminology;
  }

  /**
   * Sets the terminology.
   *
   * @param terminology the terminology
   */
  public void setTerminology(final String terminology) {
    this.terminology = terminology;
  }

  /**
   * Returns the version.
   *
   * @return the version
   */
  @Schema(description = "Terminology version, e.g. '23.11d'")
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version.
   *
   * @param version the version
   */
  public void setVersion(final String version) {
    this.version = version;
  }

  /**
   * Returns the level.
   *
   * @return the level
   */
  @Schema(
      description =
          "Level of depth in a hierarchy (when this object is used to represent an element in a"
              + " path)")
  public Integer getLevel() {
    return level;
  }

  /**
   * Sets the level.
   *
   * @param level the level
   */
  public void setLevel(final Integer level) {
    this.level = level;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((code == null) ? 0 : code.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    result = prime * result + ((level == null) ? 0 : level.hashCode());
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
    final ConceptMinimal other = (ConceptMinimal) obj;
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
    if (terminology == null) {
      if (other.terminology != null) {
        return false;
      }
    } else if (!terminology.equals(other.terminology)) {
      return false;
    }
    if (version == null) {
      if (other.version != null) {
        return false;
      }
    } else if (!version.equals(other.version)) {
      return false;
    }
    if (level == null) {
      if (other.level != null) {
        return false;
      }
    } else if (!level.equals(other.level)) {
      return false;
    }
    return true;
  }

  /* see superclass */
  @Override
  public int compareTo(ConceptMinimal o) {
    // Handle null
    return (name + code).compareToIgnoreCase(o.getName() + o.getCode());
  }
}
