
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an OWL Axiom.
 */
public class Axiom extends BaseModel {

  /** The annotated property. */
  String annotatedProperty;

  /** The annotated source. */
  String annotatedSource;

  /** The annotated target. */
  String annotatedTarget;

  /** The type. */
  String type;

  /** The relationship to target. */
  String relationshipToTarget;

  /** The target code. */
  String targetCode;

  /** The target term group. */
  String targetTermGroup;

  /** The target terminology. */
  String targetTerminology;

  /** The target terminology version. */
  String targetTerminologyVersion;

  /** The def source. */
  String defSource;

  /** The source code. */
  String sourceCode;

  /** The source date. */
  String sourceDate;

  /** The subsource name. */
  String subsourceName;

  /** The term group. */
  String termGroup;

  /** The term source. */
  String termSource;

  /** The qualifiers. */
  List<Qualifier> qualifiers;

  /**
   * Returns the annotated property.
   *
   * @return the annotated property
   */
  public String getAnnotatedProperty() {
    return annotatedProperty;
  }

  /**
   * Sets the annotated property.
   *
   * @param annotatedProperty the annotated property
   */
  public void setAnnotatedProperty(String annotatedProperty) {
    this.annotatedProperty = annotatedProperty;
  }

  /**
   * Returns the annotated source.
   *
   * @return the annotated source
   */
  public String getAnnotatedSource() {
    return annotatedSource;
  }

  /**
   * Sets the annotated source.
   *
   * @param annotatedSource the annotated source
   */
  public void setAnnotatedSource(String annotatedSource) {
    this.annotatedSource = annotatedSource;
  }

  /**
   * Returns the annotated target.
   *
   * @return the annotated target
   */
  public String getAnnotatedTarget() {
    return annotatedTarget;
  }

  /**
   * Sets the annotated target.
   *
   * @param annotatedTarget the annotated target
   */
  public void setAnnotatedTarget(String annotatedTarget) {
    this.annotatedTarget = annotatedTarget;
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
  public void setType(String type) {
    this.type = type;
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
   * Returns the relationship to target.
   *
   * @return the relationship to target
   */
  public String getRelationshipToTarget() {
    return relationshipToTarget;
  }

  /**
   * Sets the relationship to target.
   *
   * @param relationshipToTarget the relationship to target
   */
  public void setRelationshipToTarget(String relationshipToTarget) {
    this.relationshipToTarget = relationshipToTarget;
  }

  /**
   * Returns the target code.
   *
   * @return the target code
   */
  public String getTargetCode() {
    return targetCode;
  }

  /**
   * Sets the target code.
   *
   * @param targetCode the target code
   */
  public void setTargetCode(String targetCode) {
    this.targetCode = targetCode;
  }

  /**
   * Returns the target term group.
   *
   * @return the target term group
   */
  public String getTargetTermGroup() {
    return targetTermGroup;
  }

  /**
   * Sets the target term group.
   *
   * @param targetTermGroup the target term group
   */
  public void setTargetTermGroup(String targetTermGroup) {
    this.targetTermGroup = targetTermGroup;
  }

  /**
   * Returns the target terminology.
   *
   * @return the target terminology
   */
  public String getTargetTerminology() {
    return targetTerminology;
  }

  /**
   * Sets the target terminology.
   *
   * @param targetTerminology the target terminology
   */
  public void setTargetTerminology(String targetTerminology) {
    this.targetTerminology = targetTerminology;
  }

  /**
   * Returns the target terminology version.
   *
   * @return the target terminology version
   */
  public String getTargetTerminologyVersion() {
    return targetTerminologyVersion;
  }

  /**
   * Sets the target terminology version.
   *
   * @param targetTerminologyVersion the target terminology version
   */
  public void setTargetTerminologyVersion(String targetTerminologyVersion) {
    this.targetTerminologyVersion = targetTerminologyVersion;
  }

  /**
   * Returns the def source.
   *
   * @return the def source
   */
  public String getDefSource() {
    return defSource;
  }

  /**
   * Sets the def source.
   *
   * @param defSource the def source
   */
  public void setDefSource(String defSource) {
    this.defSource = defSource;
  }

  /**
   * Returns the source code.
   *
   * @return the source code
   */
  public String getSourceCode() {
    return sourceCode;
  }

  /**
   * Sets the source code.
   *
   * @param sourceCode the source code
   */
  public void setSourceCode(String sourceCode) {
    this.sourceCode = sourceCode;
  }

  /**
   * Returns the source date.
   *
   * @return the source date
   */
  public String getSourceDate() {
    return sourceDate;
  }

  /**
   * Sets the source date.
   *
   * @param sourceDate the source date
   */
  public void setSourceDate(String sourceDate) {
    this.sourceDate = sourceDate;
  }

  /**
   * Returns the subsource name.
   *
   * @return the subsource name
   */
  public String getSubsourceName() {
    return subsourceName;
  }

  /**
   * Sets the subsource name.
   *
   * @param subsourceName the subsource name
   */
  public void setSubsourceName(String subsourceName) {
    this.subsourceName = subsourceName;
  }

  /**
   * Returns the term group.
   *
   * @return the term group
   */
  public String getTermGroup() {
    return termGroup;
  }

  /**
   * Sets the term group.
   *
   * @param termGroup the term group
   */
  public void setTermGroup(String termGroup) {
    this.termGroup = termGroup;
  }

  /**
   * Returns the term source.
   *
   * @return the term source
   */
  public String getTermSource() {
    return termSource;
  }

  /**
   * Sets the term source.
   *
   * @param termSource the term source
   */
  public void setTermSource(String termSource) {
    this.termSource = termSource;
  }

}
