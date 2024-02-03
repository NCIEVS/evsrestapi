
package gov.nih.nci.evs.api.model;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents an entry in a table of associations between two concepts.
 */
@Schema(description = "Represents an entry in a table of associations between two concepts")
@JsonInclude(Include.NON_EMPTY)
public class AssociationEntry extends Relationship {

  /** the terminology */
  @Field(type = FieldType.Text)
  private String terminology;

  /** the version */
  @Field(type = FieldType.Text)
  private String version;

  /** the association */
  @Field(type = FieldType.Text)
  private String association;

  /** the code */
  @Field(type = FieldType.Text)
  private String code;

  /** the name */
  @Field(type = FieldType.Text)
  private String name;

  /** the relatedCode */
  @Field(type = FieldType.Text)
  private String relatedCode;

  /** the relatedName */
  @Field(type = FieldType.Text)
  private String relatedName;

  /**
   * Instantiates an empty {@link AssociationEntry}.
   */
  public AssociationEntry() {
    // n/a
  }

  /**
   * Instantiates a {@link AssociationEntry} from the specified parameters.
   *
   * @param other the other
   */
  public AssociationEntry(final AssociationEntry other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final AssociationEntry other) {
    super.populateFrom(other);
  }

  /**
   * @return the terminology
   */
  @Schema(description = "Terminology abbreviation, e.g. 'nci'")
  public String getTerminology() {
    return terminology;
  }

  /**
   * @param terminology the terminology to set
   */
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /**
   * @return the version
   */
  @Schema(description = "Terminology version, e.g. '23.11d'")
  public String getVersion() {
    return version;
  }

  /**
   * @param version the version to set
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * @return the association
   */
  @Schema(description = "Type of relationship between code and related code")
  public String getAssociation() {
    return association;
  }

  /**
   * @param association the association to set
   */
  public void setAssociation(String association) {
    this.association = association;
  }

  /**
   * @return the code
   */
  @Schema(description = "Code on the 'from' side of the association")
  @Override
  public String getCode() {
    return code;
  }

  /**
   * @param code the code to set
   */
  @Override
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * @return the name
   */
  @Schema(description = "Preferred name of the code")
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the relatedCode
   */
  @Schema(description = "Code on the 'to' side of the association")
  @Override
  public String getRelatedCode() {
    return relatedCode;
  }

  /**
   * @param relatedCode the relatedCode to set
   */
  @Override
  public void setRelatedCode(String relatedCode) {
    this.relatedCode = relatedCode;
  }

  /**
   * @return the relatedName
   */
  @Override
  @Schema(description = "Preferred name of the related code")
  public String getRelatedName() {
    return relatedName;
  }

  /**
   * @param relatedName the relatedName to set
   */
  @Override
  public void setRelatedName(String relatedName) {
    this.relatedName = relatedName;
  }

}
