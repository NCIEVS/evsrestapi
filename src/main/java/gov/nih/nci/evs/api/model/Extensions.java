package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

/** Represents a synonym of a concept. */
@Schema(hidden = true)
// Show the empty brackets because it's clearer what happened
@JsonInclude(Include.NON_NULL)
public class Extensions extends BaseModel implements Comparable<Extensions> {

  /** CTRP "is disease" flag. */
  @Field(type = FieldType.Keyword)
  private Boolean isDisease;

  /** The is disease grade. */
  @Field(type = FieldType.Keyword)
  private Boolean isDiseaseGrade;

  /** The is disease stage. */
  @Field(type = FieldType.Keyword)
  private Boolean isDiseaseStage;

  /** The is main type. */
  @Field(type = FieldType.Keyword)
  private Boolean isMainType;

  /** CTRP "is subtype" flag. */
  @Field(type = FieldType.Keyword)
  private Boolean isSubtype;

  /** The is biomarker. */
  @Field(type = FieldType.Keyword)
  private Boolean isBiomarker;

  /** The is reference gene. */
  @Field(type = FieldType.Keyword)
  private Boolean isReferenceGene;

  /** The paths to root. */
  @Field(type = FieldType.Object, enabled = false)
  @Mapping(enabled = false)
  private List<Paths> mainMenuAncestors;

  /** The cdisc type. grouper, codelist, concept type. */
  @Field(type = FieldType.Keyword)
  private String cdiscType;

  /**
   * The cdisc submission value. only applies to codelist type or concept type (where only one
   * CDISC/PT)
   */
  private String cdiscSubmissionValue;

  /** The cdisc submission value map. only applies to concept types */
  private Map<String, String> cdiscSubmissionValueMap;

  /**
   * The cdisc submission value code. applies to codelist type (name of the NCI/AB code for a
   * codelist)
   */
  private String cdiscSubmissionValueCode;

  /** Instantiates an empty {@link Extensions}. */
  public Extensions() {
    // n/a
  }

  /**
   * Instantiates a {@link Extensions} from the specified parameters.
   *
   * @param other the other
   */
  public Extensions(final Extensions other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Extensions other) {
    super.populateFrom(other);
    isDisease = other.getIsDisease();
    isDiseaseStage = other.getIsDiseaseStage();
    isDiseaseGrade = other.getIsDiseaseGrade();
    isMainType = other.getIsMainType();
    isSubtype = other.getIsSubtype();
    isBiomarker = other.getIsBiomarker();
    isReferenceGene = other.getIsReferenceGene();
    mainMenuAncestors = new ArrayList<>(other.getMainMenuAncestors());
  }

  /**
   * Returns the is disease.
   *
   * @return the is disease
   */
  public Boolean getIsDisease() {
    return isDisease;
  }

  /**
   * Sets the is disease.
   *
   * @param isDisease the is disease
   */
  public void setIsDisease(final Boolean isDisease) {
    this.isDisease = isDisease;
  }

  /**
   * Returns the is disease stage.
   *
   * @return the is disease stage
   */
  public Boolean getIsDiseaseStage() {
    return isDiseaseStage;
  }

  /**
   * Sets the is disease stage.
   *
   * @param isDiseaseStage the is disease stage
   */
  public void setIsDiseaseStage(final Boolean isDiseaseStage) {
    this.isDiseaseStage = isDiseaseStage;
  }

  /**
   * Returns the is disease grade.
   *
   * @return the is disease grade
   */
  public Boolean getIsDiseaseGrade() {
    return isDiseaseGrade;
  }

  /**
   * Sets the is disease grade.
   *
   * @param isDiseaseGrade the is disease grade
   */
  public void setIsDiseaseGrade(final Boolean isDiseaseGrade) {
    this.isDiseaseGrade = isDiseaseGrade;
  }

  /**
   * Returns the is main type.
   *
   * @return the is main type
   */
  public Boolean getIsMainType() {
    return isMainType;
  }

  /**
   * Sets the is main type.
   *
   * @param isMainType the is main type
   */
  public void setIsMainType(final Boolean isMainType) {
    this.isMainType = isMainType;
  }

  /**
   * Returns the is subtype.
   *
   * @return the is subtype
   */
  public Boolean getIsSubtype() {
    return isSubtype;
  }

  /**
   * Sets the is subtype.
   *
   * @param isSubtype the is subtype
   */
  public void setIsSubtype(final Boolean isSubtype) {
    this.isSubtype = isSubtype;
  }

  /**
   * Returns the is biomarker.
   *
   * @return the is biomarker
   */
  public Boolean getIsBiomarker() {
    return isBiomarker;
  }

  /**
   * Sets the is biomarker.
   *
   * @param isBiomarker the is biomarker
   */
  public void setIsBiomarker(final Boolean isBiomarker) {
    this.isBiomarker = isBiomarker;
  }

  /**
   * Returns the is reference gene.
   *
   * @return the is reference gene
   */
  public Boolean getIsReferenceGene() {
    return isReferenceGene;
  }

  /**
   * Sets the is reference gene.
   *
   * @param isReferenceGene the is reference gene
   */
  public void setIsReferenceGene(final Boolean isReferenceGene) {
    this.isReferenceGene = isReferenceGene;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((isBiomarker == null) ? 0 : isBiomarker.hashCode());
    result = prime * result + ((isDisease == null) ? 0 : isDisease.hashCode());
    result = prime * result + ((isDiseaseGrade == null) ? 0 : isDiseaseGrade.hashCode());
    result = prime * result + ((isDiseaseStage == null) ? 0 : isDiseaseStage.hashCode());
    result = prime * result + ((isMainType == null) ? 0 : isMainType.hashCode());
    result = prime * result + ((isReferenceGene == null) ? 0 : isReferenceGene.hashCode());
    result = prime * result + ((isSubtype == null) ? 0 : isSubtype.hashCode());
    result = prime * result + ((mainMenuAncestors == null) ? 0 : mainMenuAncestors.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Extensions other = (Extensions) obj;
    if (isBiomarker == null) {
      if (other.isBiomarker != null) return false;
    } else if (!isBiomarker.equals(other.isBiomarker)) return false;
    if (isDisease == null) {
      if (other.isDisease != null) return false;
    } else if (!isDisease.equals(other.isDisease)) return false;
    if (isDiseaseGrade == null) {
      if (other.isDiseaseGrade != null) return false;
    } else if (!isDiseaseGrade.equals(other.isDiseaseGrade)) return false;
    if (isDiseaseStage == null) {
      if (other.isDiseaseStage != null) return false;
    } else if (!isDiseaseStage.equals(other.isDiseaseStage)) return false;
    if (isMainType == null) {
      if (other.isMainType != null) return false;
    } else if (!isMainType.equals(other.isMainType)) return false;
    if (isReferenceGene == null) {
      if (other.isReferenceGene != null) return false;
    } else if (!isReferenceGene.equals(other.isReferenceGene)) return false;
    if (isSubtype == null) {
      if (other.isSubtype != null) return false;
    } else if (!isSubtype.equals(other.isSubtype)) return false;
    if (mainMenuAncestors == null) {
      if (other.mainMenuAncestors != null) return false;
    } else if (!mainMenuAncestors.equals(other.mainMenuAncestors)) return false;
    return true;
  }

  /**
   * Main menu ancestors.
   *
   * @return the paths
   */
  public List<Paths> getMainMenuAncestors() {
    if (mainMenuAncestors == null) {
      mainMenuAncestors = new ArrayList<>();
    }
    return mainMenuAncestors;
  }

  /**
   * Sets the main menu ancestors.
   *
   * @param mainMenuAncestors the main menu ancestors
   */
  public void setMainMenuAncestors(final List<Paths> mainMenuAncestors) {
    this.mainMenuAncestors = mainMenuAncestors;
  }

  /**
   * Returns the cdisc type.
   *
   * @return the cdisc type
   */
  public String getCdiscType() {
    return cdiscType;
  }

  /**
   * Sets the cdisc type.
   *
   * @param cdiscType the cdisc type
   */
  public void setCdiscType(String cdiscType) {
    this.cdiscType = cdiscType;
  }

  /**
   * Returns the cdisc submission value.
   *
   * @return the cdisc submission value
   */
  public String getCdiscSubmissionValue() {
    return cdiscSubmissionValue;
  }

  /**
   * Sets the cdisc submission value.
   *
   * @param cdiscSubmissionValue the cdisc submission value
   */
  public void setCdiscSubmissionValue(String cdiscSubmissionValue) {
    this.cdiscSubmissionValue = cdiscSubmissionValue;
  }

  /**
   * Returns the cdisc submission value map.
   *
   * @return the cdisc submission value map
   */
  public Map<String, String> getCdiscSubmissionValueMap() {
    if (cdiscSubmissionValueMap == null) {
      cdiscSubmissionValueMap = new HashMap<>();
    }
    return cdiscSubmissionValueMap;
  }

  /**
   * Sets the cdisc submission value map.
   *
   * @param cdiscSubmissionValueMap the cdisc submission value map
   */
  public void setCdiscSubmissionValueMap(Map<String, String> cdiscSubmissionValueMap) {
    this.cdiscSubmissionValueMap = cdiscSubmissionValueMap;
  }

  /**
   * Returns the cdisc submission value code.
   *
   * @return the cdisc submission value code
   */
  public String getCdiscSubmissionValueCode() {
    return cdiscSubmissionValueCode;
  }

  /**
   * Sets the cdisc submission value code.
   *
   * @param cdiscSubmissionValueCode the cdisc submission value code
   */
  public void setCdiscSubmissionValueCode(String cdiscSubmissionValueCode) {
    this.cdiscSubmissionValueCode = cdiscSubmissionValueCode;
  }

  /* see superclass */
  @Override
  public int compareTo(Extensions o) {
    return (o + "").compareTo(o + "");
  }
}
