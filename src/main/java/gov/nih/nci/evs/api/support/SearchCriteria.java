
package gov.nih.nci.evs.api.support;

import java.util.ArrayList;
import java.util.List;

import gov.nih.nci.evs.api.model.BaseModel;
import gov.nih.nci.evs.api.model.IncludeParam;

/**
 * Search criteria object for /concept/search implementation.
 */
public class SearchCriteria extends BaseModel {

  /** The terminology. */
  private List<String> terminology;

  /** The term. */
  private String term;

  /** The type. */
  private String type = "contains";

  /** The include. */
  private String include = "minimal";

  /** The from record. */
  private Integer fromRecord = 0;

  /** The page size. */
  private Integer pageSize = 10;

  /** The concept status. */
  private List<String> conceptStatus;

  /** The property. */
  private List<String> property;

  /** The contributing source. */
  private List<String> contributingSource;

  /** The synonym source. */
  private List<String> synonymSource;

  /** The definition source. */
  private List<String> definitionSource;

  /** The synonym term group. */
  private String synonymTermGroup;

  /** The inverse. */
  private Boolean inverse = false;

  /** The association. */
  private List<String> association;

  /** The role. */
  private List<String> role;

  /**
   * Returns the terminologies.
   *
   * @return the terminologies
   */
  public List<String> getTerminology() {
    if (terminology == null) {
      terminology = new ArrayList<>();
    }
    return terminology;
  }

  /**
   * Sets the terminology.
   *
   * @param terminology the terminology
   */
  public void setTerminology(List<String> terminology) {
    this.terminology = terminology;
  }

  /**
   * Returns the term.
   *
   * @return the term
   */
  public String getTerm() {
    return term;
  }

  /**
   * Sets the term.
   *
   * @param term the term
   */
  public void setTerm(String term) {
    this.term = term;
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
   * Returns the include.
   *
   * @return the include
   */
  public String getInclude() {
    return include;
  }

  /**
   * Sets the include.
   *
   * @param include the include
   */
  public void setInclude(String include) {
    this.include = include;
  }

  /**
   * Returns the from record.
   *
   * @return the from record
   */
  public Integer getFromRecord() {
    return fromRecord;
  }

  /**
   * Sets the from record.
   *
   * @param fromRecord the from record
   */
  public void setFromRecord(Integer fromRecord) {
    this.fromRecord = fromRecord;
  }

  /**
   * Returns the page size.
   *
   * @return the page size
   */
  public Integer getPageSize() {
    return pageSize;
  }

  /**
   * Sets the page size.
   *
   * @param pageSize the page size
   */
  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  /**
   * Returns the concept status.
   *
   * @return the concept status
   */
  public List<String> getConceptStatus() {
    if (conceptStatus == null) {
      conceptStatus = new ArrayList<>();
    }
    return conceptStatus;
  }

  /**
   * Sets the concept status.
   *
   * @param conceptStatus the concept status
   */
  public void setConceptStatus(List<String> conceptStatus) {
    this.conceptStatus = conceptStatus;
  }

  /**
   * Returns the property.
   *
   * @return the property
   */
  public List<String> getProperty() {
    if (property == null) {
      property = new ArrayList<>();
    }
    return property;
  }

  /**
   * Sets the property.
   *
   * @param property the property
   */
  public void setProperty(List<String> property) {
    this.property = property;
  }

  /**
   * Returns the contributing source.
   *
   * @return the contributing source
   */
  public List<String> getContributingSource() {
    if (contributingSource == null) {
      contributingSource = new ArrayList<>();
    }
    return contributingSource;
  }

  /**
   * Sets the contributing source.
   *
   * @param contributingSource the contributing source
   */
  public void setContributingSource(List<String> contributingSource) {
    this.contributingSource = contributingSource;
  }

  /**
   * Returns the synonym source.
   *
   * @return the synonym source
   */
  public List<String> getSynonymSource() {
    if (synonymSource == null) {
      synonymSource = new ArrayList<>();
    }
    return synonymSource;
  }

  /**
   * Sets the synonym source.
   *
   * @param synonymSource the synonym source
   */
  public void setSynonymSource(List<String> synonymSource) {
    this.synonymSource = synonymSource;
  }

  /**
   * Returns the definition source.
   *
   * @return the definition source
   */
  public List<String> getDefinitionSource() {
    if (definitionSource == null) {
      definitionSource = new ArrayList<>();
    }
    return definitionSource;
  }

  /**
   * Sets the definition source.
   *
   * @param definitionSource the definition source
   */
  public void setDefinitionSource(List<String> definitionSource) {
    this.definitionSource = definitionSource;
  }

  /**
   * Returns the synonym term group.
   *
   * @return the synonym term group
   */
  public String getSynonymTermGroup() {
    return synonymTermGroup;
  }

  /**
   * Sets the synonym term group.
   *
   * @param synonymTermGroup the synonym term group
   */
  public void setSynonymTermGroup(String synonymTermGroup) {
    this.synonymTermGroup = synonymTermGroup;
  }

  /**
   * Returns the inverse.
   *
   * @return the inverse
   */
  public Boolean getInverse() {
    return inverse;
  }

  /**
   * Sets the inverse.
   *
   * @param inverse the inverse
   */
  public void setInverse(Boolean inverse) {
    this.inverse = inverse;
  }

  /**
   * Returns the association.
   *
   * @return the association
   */
  public List<String> getAssociation() {
    if (association == null) {
      association = new ArrayList<>();
    }
    return association;
  }

  /**
   * Sets the association.
   *
   * @param association the association
   */
  public void setAssociation(List<String> association) {
    this.association = association;
  }

  /**
   * Returns the role.
   *
   * @return the role
   */
  public List<String> getRole() {
    if (role == null) {
      role = new ArrayList<>();
    }
    return role;
  }

  /**
   * Sets the role.
   *
   * @param role the role
   */
  public void setRole(List<String> role) {
    this.role = role;
  }

  /**
   * Compute include param.
   *
   * @return the include param
   */
  public IncludeParam computeIncludeParam() {
    return new IncludeParam(include);
  }

  /**
   * Check required fields.
   *
   * @return true, if successful
   */
  public boolean checkRequiredFields() {
    if (term == null) {
      return false;
    }
    return true;
  }

  /**
   * Compute missing required fields.
   *
   * @return the string
   */
  public String computeMissingRequiredFields() {
    return "term";
  }
}
