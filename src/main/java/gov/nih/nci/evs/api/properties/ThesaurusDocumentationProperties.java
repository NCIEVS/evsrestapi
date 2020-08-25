

package gov.nih.nci.evs.api.properties;

import java.util.Map;

/**
 * NCI Thesaurus Properties.
 */
public class ThesaurusDocumentationProperties {

  /** The code. */
  private String code;

  /** The preferred name. */
  private String preferredName;

  /** The synonym. */
  private String synonym;

  /** The synonym term type. */
  private String synonymTermType;

  /** The synonym source. */
  private String synonymSource;

  /** The synonym code. */
  private String synonymCode;

  /** The synonym sub source. */
  private String synonymSubSource;

  /** The definition source. */
  private String definitionSource;

  /**
   * Returns the code.
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
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * Returns the preferred name.
   *
   * @return the preferred name
   */
  public String getPreferredName() {
    return preferredName;
  }

  /**
   * Sets the preferred name.
   *
   * @param preferredName the preferred name
   */
  public void setPreferredName(String preferredName) {
    this.preferredName = preferredName;
  }

  /**
   * Returns the synonym.
   *
   * @return the synonym
   */
  public String getSynonym() {
    return synonym;
  }

  /**
   * Sets the synonym.
   *
   * @param synonym the synonym
   */
  public void setSynonym(String synonym) {
    this.synonym = synonym;
  }

  /**
   * Returns the synonym term type.
   *
   * @return the synonym term type
   */
  public String getSynonymTermType() {
    return synonymTermType;
  }

  /**
   * Sets the synonym term type.
   *
   * @param synonymTermType the synonym term type
   */
  public void setSynonymTermType(String synonymTermType) {
    this.synonymTermType = synonymTermType;
  }

  /**
   * Returns the synonym source.
   *
   * @return the synonym source
   */
  public String getSynonymSource() {
    return synonymSource;
  }

  /**
   * Sets the synonym source.
   *
   * @param synonymSource the synonym source
   */
  public void setSynonymSource(String synonymSource) {
    this.synonymSource = synonymSource;
  }

  /**
   * Returns the synonym code.
   *
   * @return the synonym code
   */
  public String getSynonymCode() {
    return synonymCode;
  }

  /**
   * Sets the synonym code.
   *
   * @param synonymCode the synonym code
   */
  public void setSynonymCode(String synonymCode) {
    this.synonymCode = synonymCode;
  }

  /**
   * Returns the synonym sub source.
   *
   * @return the synonym sub source
   */
  public String getSynonymSubSource() {
    return synonymSubSource;
  }

  /**
   * Sets the synonym sub source.
   *
   * @param synonymSubSource the synonym sub source
   */
  public void setSynonymSubSource(String synonymSubSource) {
    this.synonymSubSource = synonymSubSource;
  }

  /**
   * Returns the definition source.
   *
   * @return the definition source
   */
  public String getDefinitionSource() {
    return definitionSource;
  }

  /**
   * Sets the definition source.
   *
   * @param definitionSource the definition source
   */
  public void setDefinitionSource(String definitionSource) {
    this.definitionSource = definitionSource;
  }

  /**
   * Returns the map relation.
   *
   * @return the map relation
   */
  public String getMapRelation() {
    return mapRelation;
  }

  /**
   * Sets the map relation.
   *
   * @param mapRelation the map relation
   */
  public void setMapRelation(String mapRelation) {
    this.mapRelation = mapRelation;
  }

  /**
   * Returns the map target.
   *
   * @return the map target
   */
  public String getMapTarget() {
    return mapTarget;
  }

  /**
   * Sets the map target.
   *
   * @param mapTarget the map target
   */
  public void setMapTarget(String mapTarget) {
    this.mapTarget = mapTarget;
  }

  /**
   * Returns the map target term type.
   *
   * @return the map target term type
   */
  public String getMapTargetTermType() {
    return mapTargetTermType;
  }

  /**
   * Sets the map target term type.
   *
   * @param mapTargetTermType the map target term type
   */
  public void setMapTargetTermType(String mapTargetTermType) {
    this.mapTargetTermType = mapTargetTermType;
  }

  /**
   * Returns the map target terminology.
   *
   * @return the map target terminology
   */
  public String getMapTargetTerminology() {
    return mapTargetTerminology;
  }

  /**
   * Sets the map target terminology.
   *
   * @param mapTargetTerminology the map target terminology
   */
  public void setMapTargetTerminology(String mapTargetTerminology) {
    this.mapTargetTerminology = mapTargetTerminology;
  }

  /**
   * Returns the map target terminology version.
   *
   * @return the map target terminology version
   */
  public String getMapTargetTerminologyVersion() {
    return mapTargetTerminologyVersion;
  }

  /**
   * Sets the map target terminology version.
   *
   * @param mapTargetTerminologyVersion the map target terminology version
   */
  public void setMapTargetTerminologyVersion(String mapTargetTerminologyVersion) {
    this.mapTargetTerminologyVersion = mapTargetTerminologyVersion;
  }

  /**
   * Sets the sources.
   *
   * @param sources the sources
   */
  public void setSources(Map<String, String> sources) {
    this.sources = sources;
  }

  /** The map relation. */
  private String mapRelation;

  /** The map target. */
  private String mapTarget;

  /** The map target term type. */
  private String mapTargetTermType;

  /** The map target terminology. */
  private String mapTargetTerminology;

  /** The map target terminology version. */
  private String mapTargetTerminologyVersion;

  /** The sources - both definition and synonym and sub source. */
  private Map<String, String> sources;

  /** The term types. */
  private Map<String, String> termTypes;

  /**
   * Returns the sources.
   *
   * @return the sources
   */
  public Map<String, String> getSources() {
    return sources;
  }

  /**
   * Sets the sources.
   *
   * @param sources the sources
   */
  public void setsources(Map<String, String> sources) {
    this.sources = sources;
  }

  /**
   * Returns the term types.
   *
   * @return the term types
   */
  public Map<String, String> getTermTypes() {
    return termTypes;
  }

  /**
   * Sets the term types.
   *
   * @param termTypes the term types
   */
  public void setTermTypes(Map<String, String> termTypes) {
    this.termTypes = termTypes;
  }

}
