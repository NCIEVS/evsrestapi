
package gov.nih.nci.evs.api.model.sparql;

/**
 * Bindings for sparql queries.
 */
public class Bindings {

  /** The property. */
  private Property property;

  /** The property label. */
  private Property propertyLabel;

  /** The property code. */
  private Property propertyCode;

  /** The concept code. */
  private Property conceptCode;

  /** The concept label. */
  private Property conceptLabel;

  /** The property value. */
  private Property propertyValue;

  /** The axiom. */
  private Property axiom;

  /** The axiom property. */
  private Property axiomProperty;

  /** The axiom value. */
  private Property axiomValue;

  /** The subclass. */
  private Property subclass;

  /** The subclass code. */
  private Property subclassCode;

  /** The subclass label. */
  private Property subclassLabel;

  /** The superclass. */
  private Property superclass;

  /** The superclass code. */
  private Property superclassCode;

  /** The superclass label. */
  private Property superclassLabel;

  /** The relationship. */
  private Property relationship;

  /** The relationship code. */
  private Property relationshipCode;

  /** The related concept code. */
  private Property relatedConceptCode;

  /** The related concept label. */
  private Property relatedConceptLabel;

  /** The parent code. */
  private Property parentCode;

  /** The parent label. */
  private Property parentLabel;

  /** The child code. */
  private Property childCode;

  /** The child label. */
  private Property childLabel;

  /** The count. */
  private Property count;

  /** The score. */
  private Property score;

  /** The preferred name. */
  private Property preferredName;

  /** The concept status. */
  private Property conceptStatus;

  /** The graph name. */
  private Property graphName;

  /** The version. */
  // Support for Version Info
  private Property version;

  /** The date. */
  private Property date;

  /** The comment. */
  private Property comment;

  /** The source. */
  private Property source;

  /**
   * Returns the property value.
   *
   * @return the property value
   */
  public Property getPropertyValue() {
    return propertyValue;
  }

  /**
   * Sets the property value.
   *
   * @param propertyValue the property value
   */
  public void setPropertyValue(Property propertyValue) {
    this.propertyValue = propertyValue;
  }

  /**
   * Returns the property.
   *
   * @return the property
   */
  public Property getProperty() {
    return property;
  }

  /**
   * Sets the property.
   *
   * @param property the property
   */
  public void setProperty(Property property) {
    this.property = property;
  }

  /**
   * Returns the concept code.
   *
   * @return the concept code
   */
  public Property getConceptCode() {
    return conceptCode;
  }

  /**
   * Sets the concept code.
   *
   * @param conceptCode the concept code
   */
  public void setConceptCode(Property conceptCode) {
    this.conceptCode = conceptCode;
  }

  /**
   * Returns the property label.
   *
   * @return the property label
   */
  public Property getPropertyLabel() {
    return propertyLabel;
  }

  /**
   * Sets the property label.
   *
   * @param propertyLabel the property label
   */
  public void setPropertyLabel(Property propertyLabel) {
    this.propertyLabel = propertyLabel;
  }

  /**
   * Returns the property code.
   *
   * @return the property code
   */
  public Property getPropertyCode() {
    return propertyCode;
  }

  /**
   * Sets the property code.
   *
   * @param propertyCode the property code
   */
  public void setPropertyCode(Property propertyCode) {
    this.propertyCode = propertyCode;
  }

  /**
   * Returns the axiom.
   *
   * @return the axiom
   */
  public Property getAxiom() {
    return axiom;
  }

  /**
   * Sets the axiom.
   *
   * @param axiom the axiom
   */
  public void setAxiom(Property axiom) {
    this.axiom = axiom;
  }

  /**
   * Returns the axiom property.
   *
   * @return the axiom property
   */
  public Property getAxiomProperty() {
    return axiomProperty;
  }

  /**
   * Sets the axiom property.
   *
   * @param axiomProperty the axiom property
   */
  public void setAxiomProperty(Property axiomProperty) {
    this.axiomProperty = axiomProperty;
  }

  /**
   * Returns the axiom value.
   *
   * @return the axiom value
   */
  public Property getAxiomValue() {
    return axiomValue;
  }

  /**
   * Sets the axiom value.
   *
   * @param axiomValue the axiom value
   */
  public void setAxiomValue(Property axiomValue) {
    this.axiomValue = axiomValue;
  }

  /**
   * Returns the subclass.
   *
   * @return the subclass
   */
  public Property getSubclass() {
    return subclass;
  }

  /**
   * Sets the subclass.
   *
   * @param subclass the subclass
   */
  public void setSubclass(Property subclass) {
    this.subclass = subclass;
  }

  /**
   * Returns the subclass code.
   *
   * @return the subclass code
   */
  public Property getSubclassCode() {
    return subclassCode;
  }

  /**
   * Sets the subclass code.
   *
   * @param subclassCode the subclass code
   */
  public void setSubclassCode(Property subclassCode) {
    this.subclassCode = subclassCode;
  }

  /**
   * Returns the subclass label.
   *
   * @return the subclass label
   */
  public Property getSubclassLabel() {
    return subclassLabel;
  }

  /**
   * Sets the subclass label.
   *
   * @param subclassLabel the subclass label
   */
  public void setSubclassLabel(Property subclassLabel) {
    this.subclassLabel = subclassLabel;
  }

  /**
   * Returns the superclass.
   *
   * @return the superclass
   */
  public Property getSuperclass() {
    return superclass;
  }

  /**
   * Sets the superclass.
   *
   * @param superclass the superclass
   */
  public void setSuperclass(Property superclass) {
    this.superclass = superclass;
  }

  /**
   * Returns the superclass code.
   *
   * @return the superclass code
   */
  public Property getSuperclassCode() {
    return superclassCode;
  }

  /**
   * Sets the superclass code.
   *
   * @param superclassCode the superclass code
   */
  public void setSuperclassCode(Property superclassCode) {
    this.superclassCode = superclassCode;
  }

  /**
   * Returns the superclass label.
   *
   * @return the superclass label
   */
  public Property getSuperclassLabel() {
    return superclassLabel;
  }

  /**
   * Sets the superclass label.
   *
   * @param superclassLabel the superclass label
   */
  public void setSuperclassLabel(Property superclassLabel) {
    this.superclassLabel = superclassLabel;
  }

  /**
   * To string.
   *
   * @return the string
   */
  @Override
  public String toString() {
    return "ClassPojo [propertyValue = " + propertyValue + ", property = " + property
        + ", propertyLabel = " + propertyLabel + ", propertyCode = " + propertyCode + "]";
  }

  /**
   * Returns the concept label.
   *
   * @return the concept label
   */
  public Property getConceptLabel() {
    return conceptLabel;
  }

  /**
   * Sets the concept label.
   *
   * @param conceptLabel the concept label
   */
  public void setConceptLabel(Property conceptLabel) {
    this.conceptLabel = conceptLabel;
  }

  /**
   * Returns the relationship.
   *
   * @return the relationship
   */
  public Property getRelationship() {
    return relationship;
  }

  /**
   * Sets the relationship.
   *
   * @param relationship the relationship
   */
  public void setRelationship(Property relationship) {
    this.relationship = relationship;
  }

  /**
   * Returns the relationship code.
   *
   * @return the relationship code
   */
  public Property getRelationshipCode() {
    return relationshipCode;
  }

  /**
   * Sets the relationship code.
   *
   * @param relationshipCode the relationship code
   */
  public void setRelationshipCode(Property relationshipCode) {
    this.relationshipCode = relationshipCode;
  }

  /**
   * Returns the related concept code.
   *
   * @return the related concept code
   */
  public Property getRelatedConceptCode() {
    return relatedConceptCode;
  }

  /**
   * Sets the related concept code.
   *
   * @param relatedConceptCode the related concept code
   */
  public void setRelatedConceptCode(Property relatedConceptCode) {
    this.relatedConceptCode = relatedConceptCode;
  }

  /**
   * Returns the related concept label.
   *
   * @return the related concept label
   */
  public Property getRelatedConceptLabel() {
    return relatedConceptLabel;
  }

  /**
   * Sets the related concept label.
   *
   * @param relatedConceptLabel the related concept label
   */
  public void setRelatedConceptLabel(Property relatedConceptLabel) {
    this.relatedConceptLabel = relatedConceptLabel;
  }

  /**
   * Returns the parent code.
   *
   * @return the parent code
   */
  public Property getParentCode() {
    return parentCode;
  }

  /**
   * Sets the parent code.
   *
   * @param parentCode the parent code
   */
  public void setParentCode(Property parentCode) {
    this.parentCode = parentCode;
  }

  /**
   * Returns the parent label.
   *
   * @return the parent label
   */
  public Property getParentLabel() {
    return parentLabel;
  }

  /**
   * Sets the parent label.
   *
   * @param parentLabel the parent label
   */
  public void setParentLabel(Property parentLabel) {
    this.parentLabel = parentLabel;
  }

  /**
   * Returns the child code.
   *
   * @return the child code
   */
  public Property getChildCode() {
    return childCode;
  }

  /**
   * Sets the child code.
   *
   * @param childCode the child code
   */
  public void setChildCode(Property childCode) {
    this.childCode = childCode;
  }

  /**
   * Returns the child label.
   *
   * @return the child label
   */
  public Property getChildLabel() {
    return childLabel;
  }

  /**
   * Sets the child label.
   *
   * @param childLabel the child label
   */
  public void setChildLabel(Property childLabel) {
    this.childLabel = childLabel;
  }

  /**
   * Returns the count.
   *
   * @return the count
   */
  public Property getCount() {
    return count;
  }

  /**
   * Sets the count.
   *
   * @param count the count
   */
  public void setCount(Property count) {
    this.count = count;
  }

  /**
   * Returns the score.
   *
   * @return the score
   */
  public Property getScore() {
    return score;
  }

  /**
   * Sets the score.
   *
   * @param score the score
   */
  public void setScore(Property score) {
    this.score = score;
  }

  /**
   * Returns the preferred name.
   *
   * @return the preferred name
   */
  public Property getPreferredName() {
    return preferredName;
  }

  /**
   * Sets the preferred name.
   *
   * @param preferredName the preferred name
   */
  public void setPreferredName(Property preferredName) {
    this.preferredName = preferredName;
  }

  /**
   * Returns the concept status.
   *
   * @return the concept status
   */
  public Property getConceptStatus() {
    return conceptStatus;
  }

  /**
   * Sets the concept status.
   *
   * @param conceptStatus the concept status
   */
  public void setConceptStatus(Property conceptStatus) {
    this.conceptStatus = conceptStatus;
  }

  /**
   * Returns the graph name.
   *
   * @return the graph name
   */
  public Property getGraphName() {
    return graphName;
  }

  /**
   * Sets the graph name.
   *
   * @param graphName the graph name
   */
  public void setGraphName(Property graphName) {
    this.graphName = graphName;
  }

  /**
   * Returns the version.
   *
   * @return the version
   */
  public Property getVersion() {
    return version;
  }

  /**
   * Sets the version.
   *
   * @param version the version
   */
  public void setVersion(Property version) {
    this.version = version;
  }

  /**
   * Returns the date.
   *
   * @return the date
   */
  public Property getDate() {
    return date;
  }

  /**
   * Sets the date.
   *
   * @param date the date
   */
  public void setDate(Property date) {
    this.date = date;
  }

  /**
   * Returns the comment.
   *
   * @return the comment
   */
  public Property getComment() {
    return comment;
  }

  /**
   * Sets the comment.
   *
   * @param comment the comment
   */
  public void setComment(Property comment) {
    this.comment = comment;
  }

  /**
   * Returns the source.
   *
   * @return the source
   */
  public Property getSource() {
    return source;
  }

  /**
   * Sets the source.
   *
   * @param source the source
   */
  public void setSource(Property source) {
    this.source = source;
  }
}