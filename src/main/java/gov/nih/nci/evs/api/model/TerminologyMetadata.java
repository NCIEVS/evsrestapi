
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents terminology metadata, which includes information about mapping from OWL structures to
 * the terminology model.
 */
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TerminologyMetadata extends BaseModel {

  /** The ui label. */
  private String uiLabel;

  /** The max versions. */
  private Integer maxVersions;

  /** The loader. */
  private String loader;

  /** The code. */
  private String code;

  /** The concept status. */
  private String conceptStatus;

  /** The concept statuses. */
  private List<String> conceptStatuses;

  /** The retired status value. */
  private String retiredStatusValue;

  /** The preferred name. */
  private String preferredName;

  /** The relationship to target. */
  private String relationshipToTarget;

  /** The synonym. */
  private Set<String> synonym;

  /** The synonym term type. */
  private String synonymTermType;

  /** The synonym source. */
  private String synonymSource;

  /** The synonym code. */
  private String synonymCode;

  /** The synonym sub source. */
  private String synonymSubSource;

  /** The definition. */
  private Set<String> definition;

  /** The definition source. */
  private String definitionSource;

  /** The map relation. */
  private String mapRelation;

  /** The map. */
  private String map;

  /** The map target. */
  private String mapTarget;

  /** The map target term type. */
  private String mapTargetTermType;

  /** The map target terminology. */
  private String mapTargetTerminology;

  /** The map target terminology version. */
  private String mapTargetTerminologyVersion;

  /** The sources. */
  private Map<String, String> sources;

  /** The details columns. */
  private Map<String, Boolean> detailsColumns;

  /** The hierarchy flag. */
  private Boolean hierarchy;

  /** The history flag. */
  private Boolean history;

  /** The mapsets flag. */
  private Boolean mapsets;

  /** The source ct. */
  @SuppressWarnings("unused")
  private int sourceCt;

  /** The definition source set. */
  private Set<String> definitionSourceSet;

  /** The synonym source set. */
  private Set<String> synonymSourceSet;

  /** The term types. */
  private Map<String, String> termTypes;

  /** The subset link prefix. */
  private String subsetPrefix;

  /** The subset links. */
  private String subsetLink;

  /** The sources to remove. */
  private Set<String> sourcesToRemove;

  /** The subsetMembers for association entries. */
  private Set<String> subsetMember;

  /** The unpublished codes. */
  private Set<String> unpublished;

  /** The monthly db. */
  private String monthlyDb;

  /** The subset. */
  private Set<String> subset;

  /** The license text. */
  private String licenseText;

  /** The license fail text. */
  private String licenseFailText;

  /** The license check. */
  private String licenseCheck;

  /** The meta concept field. */
  private String metaConceptField;

  /** The preferred term types. */
  private List<String> preferredTermTypes;

  /** The code label. */
  private String codeLabel;

  /** The welcome text. */
  private String welcomeText;

  /**
   * Instantiates an empty {@link TerminologyMetadata}.
   */
  public TerminologyMetadata() {
    // n/a
  }

  /**
   * Instantiates a {@link TerminologyMetadata} from the specified parameters.
   *
   * @param other the other
   */
  public TerminologyMetadata(final TerminologyMetadata other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final TerminologyMetadata other) {
    super.populateFrom(other);
    uiLabel = other.getUiLabel();
    maxVersions = other.getMaxVersions();
    loader = other.getLoader();
    code = other.getCode();
    conceptStatus = other.getConceptStatus();
    conceptStatuses = new ArrayList<>(other.getConceptStatuses());
    retiredStatusValue = other.getRetiredStatusValue();
    definitionSource = other.getDefinitionSource();
    definition = new HashSet<>(other.getDefinition());
    mapRelation = other.getMapRelation();
    map = other.getMap();
    mapTarget = other.getMapTarget();
    mapTargetTerminology = other.getMapTargetTerminology();
    mapTargetTerminologyVersion = other.getMapTargetTerminologyVersion();
    mapTargetTermType = other.getMapTargetTermType();
    preferredName = other.getPreferredName();
    relationshipToTarget = other.getRelationshipToTarget();
    sources = new HashMap<>(other.getSources());
    detailsColumns = new HashMap<>(other.getDetailsColumns());
    sourceCt = sources.size();
    hierarchy = other.getHierarchy();
    history = other.getHistory();
    mapsets = other.getMapsets();
    sourcesToRemove = new HashSet<>(other.getSourcesToRemove());
    synonym = new HashSet<>(other.getSynonym());
    synonymCode = other.getSynonymCode();
    synonymSource = other.getSynonymSource();
    synonymSubSource = other.getSynonymSubSource();
    synonymTermType = other.getSynonymTermType();
    subsetPrefix = other.getSubsetPrefix();
    definitionSourceSet = new HashSet<>(other.getDefinitionSourceSet());
    synonymSourceSet = new HashSet<>(other.getSynonymSourceSet());
    termTypes = new HashMap<>(other.getTermTypes());
    subsetLink = other.getSubsetLink();
    subsetMember = new HashSet<>(other.getSubsetMember());
    unpublished = new HashSet<>(other.getUnpublished());
    monthlyDb = other.getMonthlyDb();
    licenseText = other.getLicenseText();
    licenseFailText = other.getLicenseFailText();
    licenseCheck = other.getLicenseCheck();
    metaConceptField = other.getMetaConceptField();
    preferredTermTypes = new ArrayList<>(other.getPreferredTermTypes());
    subset = new HashSet<>(other.getSubset());
    codeLabel = other.getCodeLabel();
    welcomeText = other.getWelcomeText();
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((uiLabel == null) ? 0 : uiLabel.hashCode());
    result = prime * result + ((maxVersions == null) ? 0 : maxVersions.hashCode());
    result = prime * result + ((loader == null) ? 0 : loader.hashCode());
    result = prime * result + ((code == null) ? 0 : code.hashCode());
    result = prime * result + ((definitionSource == null) ? 0 : definitionSource.hashCode());
    result = prime * result + ((mapRelation == null) ? 0 : mapRelation.hashCode());
    result = prime * result + ((mapTarget == null) ? 0 : mapTarget.hashCode());
    result = prime * result + ((mapTargetTermType == null) ? 0 : mapTargetTermType.hashCode());
    result = prime * result + ((mapTargetTerminology == null) ? 0 : mapTargetTerminology.hashCode());
    result = prime * result + ((mapTargetTerminologyVersion == null) ? 0 : mapTargetTerminologyVersion.hashCode());
    result = prime * result + ((preferredName == null) ? 0 : preferredName.hashCode());
    result = prime * result + ((sources == null) ? 0 : sources.hashCode());
    result = prime * result + ((sourcesToRemove == null) ? 0 : sourcesToRemove.hashCode());
    result = prime * result + ((synonym == null) ? 0 : synonym.hashCode());
    result = prime * result + ((synonymCode == null) ? 0 : synonymCode.hashCode());
    result = prime * result + ((synonymSource == null) ? 0 : synonymSource.hashCode());
    result = prime * result + ((synonymSubSource == null) ? 0 : synonymSubSource.hashCode());
    result = prime * result + ((synonymTermType == null) ? 0 : synonymTermType.hashCode());
    result = prime * result + ((definitionSourceSet == null) ? 0 : definitionSourceSet.hashCode());
    result = prime * result + ((synonymSourceSet == null) ? 0 : synonymSourceSet.hashCode());
    result = prime * result + ((termTypes == null) ? 0 : termTypes.hashCode());
    result = prime * result + ((subsetMember == null) ? 0 : subsetMember.hashCode());
    result = prime * result + ((unpublished == null) ? 0 : unpublished.hashCode());
    result = prime * result + ((monthlyDb == null) ? 0 : monthlyDb.hashCode());
    result = prime * result + ((licenseText == null) ? 0 : licenseText.hashCode());
    result = prime * result + ((licenseFailText == null) ? 0 : licenseFailText.hashCode());
    result = prime * result + ((licenseCheck == null) ? 0 : licenseCheck.hashCode());
    result = prime * result + ((metaConceptField == null) ? 0 : metaConceptField.hashCode());
    result = prime * result + ((preferredTermTypes == null) ? 0 : preferredTermTypes.hashCode());
    result = prime * result + ((subset == null) ? 0 : subset.hashCode());
    result = prime * result + ((subsetLink == null) ? 0 : subsetLink.hashCode());
    result = prime * result + ((subsetPrefix == null) ? 0 : subsetPrefix.hashCode());
    result = prime * result + ((codeLabel == null) ? 0 : codeLabel.hashCode());
    result = prime * result + ((welcomeText == null) ? 0 : welcomeText.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TerminologyMetadata other = (TerminologyMetadata) obj;
    if (uiLabel == null) {
      if (other.uiLabel != null)
        return false;
    } else if (!uiLabel.equals(other.uiLabel))
      return false;
    if (maxVersions == null) {
      if (other.maxVersions != null)
        return false;
    } else if (!maxVersions.equals(other.maxVersions))
      return false;
    if (loader == null) {
      if (other.loader != null)
        return false;
    } else if (!loader.equals(other.loader))
      return false;
    if (code == null) {
      if (other.code != null)
        return false;
    } else if (!code.equals(other.code))
      return false;
    if (definitionSource == null) {
      if (other.definitionSource != null)
        return false;
    } else if (!definitionSource.equals(other.definitionSource))
      return false;
    if (mapRelation == null) {
      if (other.mapRelation != null)
        return false;
    } else if (!mapRelation.equals(other.mapRelation))
      return false;
    if (mapTarget == null) {
      if (other.mapTarget != null)
        return false;
    } else if (!mapTarget.equals(other.mapTarget))
      return false;
    if (mapTargetTermType == null) {
      if (other.mapTargetTermType != null)
        return false;
    } else if (!mapTargetTermType.equals(other.mapTargetTermType))
      return false;
    if (mapTargetTerminology == null) {
      if (other.mapTargetTerminology != null)
        return false;
    } else if (!mapTargetTerminology.equals(other.mapTargetTerminology))
      return false;
    if (mapTargetTerminologyVersion == null) {
      if (other.mapTargetTerminologyVersion != null)
        return false;
    } else if (!mapTargetTerminologyVersion.equals(other.mapTargetTerminologyVersion))
      return false;
    if (preferredName == null) {
      if (other.preferredName != null)
        return false;
    } else if (!preferredName.equals(other.preferredName))
      return false;
    if (sources == null) {
      if (other.sources != null)
        return false;
    } else if (!sources.equals(other.sources))
      return false;
    if (sourcesToRemove == null) {
      if (other.sourcesToRemove != null)
        return false;
    } else if (!sourcesToRemove.equals(other.sourcesToRemove))
      return false;
    if (synonym == null) {
      if (other.synonym != null)
        return false;
    } else if (!synonym.equals(other.synonym))
      return false;
    if (synonymCode == null) {
      if (other.synonymCode != null)
        return false;
    } else if (!synonymCode.equals(other.synonymCode))
      return false;
    if (synonymSource == null) {
      if (other.synonymSource != null)
        return false;
    } else if (!synonymSource.equals(other.synonymSource))
      return false;
    if (synonymSubSource == null) {
      if (other.synonymSubSource != null)
        return false;
    } else if (!synonymSubSource.equals(other.synonymSubSource))
      return false;
    if (synonymTermType == null) {
      if (other.synonymTermType != null)
        return false;
    } else if (!synonymTermType.equals(other.synonymTermType))
      return false;
    if (definitionSourceSet == null) {
      if (other.definitionSourceSet != null)
        return false;
    } else if (!definitionSourceSet.equals(other.definitionSourceSet))
      return false;
    if (synonymSourceSet == null) {
      if (other.synonymSourceSet != null)
        return false;
    } else if (!synonymSourceSet.equals(other.synonymSourceSet))
      return false;
    if (termTypes == null) {
      if (other.termTypes != null)
        return false;
    } else if (!termTypes.equals(other.termTypes))
      return false;
    if (subsetMember == null) {
      if (other.subsetMember != null)
        return false;
    } else if (!subsetMember.equals(other.subsetMember))
      return false;
    if (unpublished == null) {
      if (other.unpublished != null)
        return false;
    } else if (!unpublished.equals(other.unpublished))
      return false;
    if (monthlyDb == null) {
      if (other.monthlyDb != null)
        return false;
    } else if (!monthlyDb.equals(other.monthlyDb))
      return false;
    if (licenseText == null) {
      if (other.licenseText != null)
        return false;
    } else if (!licenseText.equals(other.licenseText))
      return false;
    if (licenseFailText == null) {
      if (other.licenseFailText != null)
        return false;
    } else if (!licenseFailText.equals(other.licenseFailText))
      return false;
    if (licenseCheck == null) {
      if (other.licenseCheck != null)
        return false;
    } else if (!licenseCheck.equals(other.licenseCheck))
      return false;
    if (metaConceptField == null) {
      if (other.metaConceptField != null)
        return false;
    } else if (!metaConceptField.equals(other.metaConceptField))
      return false;
    if (preferredTermTypes == null) {
      if (other.preferredTermTypes != null)
        return false;
    } else if (!preferredTermTypes.equals(other.preferredTermTypes))
      return false;
    if (subset == null) {
      if (other.subset != null)
        return false;
    } else if (!subset.equals(other.subset))
      return false;
    if (subsetLink == null) {
      if (other.subsetLink != null)
        return false;
    } else if (!subsetLink.equals(other.subsetLink))
      return false;
    if (subsetPrefix == null) {
      if (other.subsetPrefix != null)
        return false;
    } else if (!subsetPrefix.equals(other.subsetPrefix))
      return false;
    if (codeLabel == null) {
      if (other.codeLabel != null)
        return false;
    } else if (!codeLabel.equals(other.codeLabel))
      return false;
    if (welcomeText == null) {
      if (other.welcomeText != null)
        return false;
    } else if (!welcomeText.equals(other.welcomeText))
      return false;
    return true;
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
   * Returns the ui label.
   *
   * @return the ui label
   */
  public String getUiLabel() {
    return uiLabel;
  }

  /**
   * Sets the ui label.
   *
   * @param uiLabel the ui label
   */
  public void setUiLabel(String uiLabel) {
    this.uiLabel = uiLabel;
  }

  /**
   * Returns the max versions.
   *
   * @return the max versions
   */
  public Integer getMaxVersions() {
    return maxVersions;
  }

  /**
   * Sets the max versions.
   *
   * @param maxVersions the max versions
   */
  public void setMaxVersions(Integer maxVersions) {
    this.maxVersions = maxVersions;
  }

  /**
   * Returns the loader.
   *
   * @return the loader
   */
  public String getLoader() {
    return loader;
  }

  /**
   * Sets the loader.
   *
   * @param loader the loader
   */
  public void setLoader(String loader) {
    this.loader = loader;
  }

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
   * Returns the concept status.
   *
   * @return the concept status
   */
  public String getConceptStatus() {
    return conceptStatus;
  }

  /**
   * Sets the concept status.
   *
   * @param conceptStatus the concept status
   */
  public void setConceptStatus(String conceptStatus) {
    this.conceptStatus = conceptStatus;
  }

  /**
   * Returns the concept statuses.
   *
   * @return the concept statuses
   */
  public List<String> getConceptStatuses() {
    if (conceptStatuses == null) {
      conceptStatuses = new ArrayList<>();
    }
    return conceptStatuses;
  }

  /**
   * Sets the concept statuses.
   *
   * @param conceptStatuses the concept statuses
   */
  public void setConceptStatuses(List<String> conceptStatuses) {
    this.conceptStatuses = conceptStatuses;
  }

  /**
   * Returns the retired status value.
   *
   * @return the retired status value
   */
  public String getRetiredStatusValue() {
    return retiredStatusValue;
  }

  /**
   * Sets the retired status value.
   *
   * @param retiredStatusValue the retired status value
   */
  public void setRetiredStatusValue(String retiredStatusValue) {
    this.retiredStatusValue = retiredStatusValue;
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
  public Set<String> getSynonym() {
    if (synonym == null) {
      synonym = new HashSet<>();
    }
    return synonym;
  }

  /**
   * Sets the synonym.
   *
   * @param synonym the synonym
   */
  public void setSynonym(Set<String> synonym) {
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
   * Sets the synonym term group. This is a bridge to support naming convention normalization.
   *
   * @param synonymTermGroup the synonym term group
   */
  public void setSynonymTermGroup(String synonymTermGroup) {
    this.synonymTermType = synonymTermGroup;
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
   * Returns the definition.
   *
   * @return the definition
   */
  public Set<String> getDefinition() {
    if (definition == null) {
      definition = new HashSet<>();
    }
    return definition;
  }

  /**
   * Sets the definition.
   *
   * @param definition the definition
   */
  public void setDefinition(Set<String> definition) {
    this.definition = definition;
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
   * Returns the map.
   *
   * @return the map
   */
  public String getMap() {
    return map;
  }

  /**
   * Sets the map.
   *
   * @param map the map
   */
  public void setMap(String map) {
    this.map = map;
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
   * Sets the map target term group. This is a bridge to support naming convention normalization.
   *
   * @param mapTargetTermGroup the map target term group
   */
  public void setMapTargetTermGroup(String mapTargetTermGroup) {
    this.mapTargetTermType = mapTargetTermGroup;
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
   * Returns the sources.
   *
   * @return the sources
   */
  public Map<String, String> getSources() {
    if (sources == null) {
      sources = new HashMap<>();
    }
    return sources;
  }

  /**
   * Returns the source ct.
   *
   * @return the source ct
   */
  public int getSourceCt() {
    return sourceCt;
  }

  /**
   * Sets the source ct.
   *
   * @param sourceCt the source ct
   */
  public void setSourceCt(final int sourceCt) {
    this.sourceCt = sourceCt;
  }

  /**
   * Sets the sources.
   *
   * @param sources the sources
   */
  public void setSources(Map<String, String> sources) {
    this.sources = sources;
  }

  /**
   * Returns the details columns.
   *
   * @return the details columns
   */
  public Map<String, Boolean> getDetailsColumns() {
    if (detailsColumns == null) {
      detailsColumns = new HashMap<>();
    }
    return detailsColumns;
  }

  /**
   * Sets the details columns.
   *
   * @param detailsColumns the details columns
   */
  public void setDetailsColumns(Map<String, Boolean> detailsColumns) {
    this.detailsColumns = detailsColumns;
  }

  /**
   * Returns the definition source set.
   *
   * @return the definition source set
   */
  public Set<String> getDefinitionSourceSet() {
    if (definitionSourceSet == null) {
      definitionSourceSet = new HashSet<>();
    }
    return definitionSourceSet;
  }

  /**
   * Sets the definition source set.
   *
   * @param definitionSourceSet the definition source set
   */
  public void setDefinitionSourceSet(Set<String> definitionSourceSet) {
    this.definitionSourceSet = definitionSourceSet;
  }

  /**
   * Returns the synonym source set.
   *
   * @return the synonym source set
   */
  public Set<String> getSynonymSourceSet() {
    if (synonymSourceSet == null) {
      synonymSourceSet = new HashSet<>();
    }
    return synonymSourceSet;
  }

  /**
   * Sets the synonym source set.
   *
   * @param synonymSourceSet the synonym source set
   */
  public void setSynonymSourceSet(Set<String> synonymSourceSet) {
    this.synonymSourceSet = synonymSourceSet;
  }

  /**
   * Returns the term types.
   *
   * @return the term types
   */
  public Map<String, String> getTermTypes() {
    if (termTypes == null) {
      termTypes = new HashMap<>();
    }
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

  /**
   * Sets the term groups. This is a bridge to support naming convention normalization.
   *
   * @param termGroups the term groups
   */
  public void setTermGroups(Map<String, String> termGroups) {
    this.termTypes = termGroups;
  }

  /**
   * Returns the subset link property.
   *
   * @return the subset link property
   */
  public String getSubsetLink() {
    return subsetLink;
  }

  /**
   * Sets the subset link property.
   *
   * @param subsetLink the subset link property
   */
  public void setSubsetLink(String subsetLink) {
    this.subsetLink = subsetLink;
  }

  /**
   * Returns the subset prefix.
   *
   * @return the subsetPrefix
   */
  public String getSubsetPrefix() {
    return subsetPrefix;
  }

  /**
   * Sets the subset prefix.
   *
   * @param subsetPrefix the subsetPrefix to set
   */
  public void setSubsetPrefix(String subsetPrefix) {
    this.subsetPrefix = subsetPrefix;
  }

  /**
   * Returns the sources to remove.
   *
   * @return the sources to remove
   */
  public Set<String> getSourcesToRemove() {
    if (sourcesToRemove == null) {
      sourcesToRemove = new HashSet<>();
    }
    return sourcesToRemove;
  }

  /**
   * Sets the sources to remove.
   *
   * @param sourcesToRemove the sources to remove
   */
  public void setSourcesToRemove(Set<String> sourcesToRemove) {
    this.sourcesToRemove = sourcesToRemove;
  }

  /**
   * Returns the subset member.
   *
   * @return the subsetMember
   */
  public Set<String> getSubsetMember() {
    if (subsetMember == null) {
      subsetMember = new HashSet<>();
    }
    return subsetMember;
  }

  /**
   * Sets the subset member.
   *
   * @param subsetMember the subsetMember to set
   */
  public void setSubsetMember(Set<String> subsetMember) {
    this.subsetMember = subsetMember;
  }

  /**
   * Returns the unpublished properties or qualifiers. These are things where the data defines the
   * metadata but then no uses of that metadata occur.
   *
   * @return the unpublished
   */
  public Set<String> getUnpublished() {
    if (unpublished == null) {
      unpublished = new HashSet<>();
    }
    return unpublished;
  }

  /**
   * Sets the unpublished.
   *
   * @param unpublished the unpublished to set
   */
  public void setUnpublished(Set<String> unpublished) {
    this.unpublished = unpublished;
  }

  /**
   * Returns the monthly db.
   *
   * @return the monthly db
   */
  public String getMonthlyDb() {
    return monthlyDb;
  }

  /**
   * Sets the monthly db.
   *
   * @param monthlyDb the monthly db
   */
  public void setMonthlyDb(String monthlyDb) {
    this.monthlyDb = monthlyDb;
  }

  /**
   * Returns the license text.
   *
   * @return the license text
   */
  public String getLicenseText() {
    return licenseText;
  }

  /**
   * Sets the license text.
   *
   * @param licenseText the license text
   */
  public void setLicenseText(String licenseText) {
    this.licenseText = licenseText;
  }

  /**
   * Returns the license fail text.
   *
   * @return the license fail text
   */
  public String getLicenseFailText() {
    return licenseFailText;
  }

  /**
   * Sets the license fail text.
   *
   * @param licenseFailText the license fail text
   */
  public void setLicenseFailText(String licenseFailText) {
    this.licenseFailText = licenseFailText;
  }

  /**
   * Returns the license check.
   *
   * @return the license check
   */
  public String getLicenseCheck() {
    return licenseCheck;
  }

  /**
   * Sets the license check.
   *
   * @param licenseCheck the license check
   */
  public void setLicenseCheck(String licenseCheck) {
    this.licenseCheck = licenseText;
  }

  /**
   * Returns the meta concept field.
   *
   * @return the meta concept field
   */
  public String getMetaConceptField() {
    return metaConceptField;
  }

  /**
   * Sets the meta concept field.
   *
   * @param metaConceptField the meta concept field
   */
  public void setMetaConceptField(String metaConceptField) {
    this.metaConceptField = metaConceptField;
  }

  /**
   * Returns the preferred term types.
   *
   * @return the preferred term types
   */
  public List<String> getPreferredTermTypes() {
    if (preferredTermTypes == null) {
      preferredTermTypes = new ArrayList<>();
    }
    return preferredTermTypes;
  }

  /**
   * Sets the preferred term types.
   *
   * @param preferredTermTypes the preferred term types
   */
  public void setPreferredTermTypes(List<String> preferredTermTypes) {
    this.preferredTermTypes = preferredTermTypes;
  }

  /**
   * Returns the code label.
   *
   * @return the codeLabel
   */
  public String getCodeLabel() {
    return codeLabel;
  }

  /**
   * Sets the code label.
   *
   * @param codeLabel the codeLabel to set
   */
  public void setCodeLabel(String codeLabel) {
    this.codeLabel = codeLabel;
  }

  /**
   * Returns the welcome text.
   *
   * @return the welcomeText
   */
  public String getWelcomeText() {
    return welcomeText;
  }

  /**
   * Sets the welcome text.
   *
   * @param welcomeText the welcomeText to set
   */
  public void setWelcomeText(String welcomeText) {
    this.welcomeText = welcomeText;
  }

  /**
   * Sets the preferred term groups. This is a bridge to support naming convention normalization.
   *
   * @param preferredTermGroups the preferred term groups
   */
  public void setPreferredTermGroups(List<String> preferredTermGroups) {
    this.preferredTermTypes = preferredTermGroups;
  }

  /**
   * Returns the subset.
   *
   * @return the subset
   */
  public Set<String> getSubset() {
    if (subset == null) {
      subset = new HashSet<>();
    }
    return subset;
  }

  /**
   * Sets the subset.
   *
   * @param subset the subset to set
   */
  public void setSubset(Set<String> subset) {
    this.subset = subset;
  }

  /**
   * Returns the hierarchy.
   *
   * @return the hierarchy
   */
  public Boolean getHierarchy() {
    if (hierarchy == null) {
      hierarchy = false;
    }
    return hierarchy;
  }

  /**
   * Sets the hierarchy.
   *
   * @param hierarchy the hierarchy to set
   */
  public void setHierarchy(Boolean hierarchy) {
    this.hierarchy = hierarchy;
  }

  /**
   * Returns the history.
   *
   * @return the history
   */
  public Boolean getHistory() {
    if (history == null) {
      history = false;
    }
    return history;
  }

  /**
   * Sets the history.
   *
   * @param history the history to set
   */
  public void setHistory(Boolean history) {
    this.history = history;
  }

  /**
   * Returns the mapsets.
   *
   * @return the mapsets
   */
  public Boolean getMapsets() {
    return mapsets;
  }

  /**
   * Sets the mapsets.
   *
   * @param mapsets the mapsets to set
   */
  public void setMapsets(Boolean mapsets) {
    this.mapsets = mapsets;
  }

  /**
   * Indicates whether or not property exclusion is the case.
   *
   * @param code the code
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isRemodeledProperty(final String code) {
    // IT was requested that Maps_To remain as property metadata for NCIt
    // to accommodate report writer use cases
    // || getMap().equals(code)
    if (code == null) {
      return false;
    }
    return getSynonym().contains(code) || getDefinition().contains(code) || code.equals(this.code)
        || code.equals(subsetLink);

  }

  /**
   * Indicates whether or not remodeled qualifier is the case.
   *
   * @param code the code
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isRemodeledQualifier(final String code) {
    if (code == null) {
      return false;
    }
    return code.equals(synonymTermType) || code.equals(synonymSource) || code.equals(synonymCode)
        || code.equals(synonymSubSource) || code.equals(definitionSource) || code.equals(mapRelation)
        || code.equals(mapTarget) || code.equals(mapTargetTermType) || code.equals(mapTargetTerminology)
        || code.equals(mapTargetTerminologyVersion);
  }

  /**
   * Indicates whether code is unpublished.
   *
   * @param code the code
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isUnpublished(final String code) {
    return getUnpublished().contains(code);
  }

  /**
   * Returns the remodeled as type.
   *
   * @param prop the prop
   * @param qual the qual
   * @param md the md
   * @return the remodeled as type
   */
  public String getRemodeledAsType(Property prop, Qualifier qual, TerminologyMetadata md) {
    String code = null;
    if (prop != null) {
      code = prop.getCode();
    } else if (qual != null) {
      code = qual.getCode();
    } else
      return " other";

    if (code.equals(preferredName)) {
      return "a preferred name";
    } else if (code.equals(relationshipToTarget)) {
      return "a relationship to target";
    } else if (code.equals(synonymTermType)) {
      return "a synonym term type";
    } else if (code.equals(synonymSource)) {
      return "a synonym source";
    } else if (code.equals(synonymCode)) {
      return "a synonym code";
    } else if (code.equals(synonymSubSource)) {
      return "a synonym subsource";
    } else if (code.equals(definitionSource)) {
      return "a definition source";
    } else if (code.equals(mapRelation)) {
      return "a map relation";
    } else if (code.equals(map)) {
      return "a map";
    } else if (code.equals(mapTarget)) {
      return "a map target";
    } else if (code.equals(mapTargetTermType)) {
      return "a map target term type";
    } else if (code.equals(mapTargetTerminology)) {
      return "a map target terminology";
    } else if (code.equals(mapTargetTerminologyVersion)) {
      return "a map target terminology version";
    } else if (md.getDefinition().contains(code)) {
      return "a definition";
    } else if (md.getSynonym().contains(code)) {
      return "a synonym";
    } else if (code.equals(this.code)) {
      return "a concept code";
    } else if (prop != null && prop.getValue() != null) {
      return prop.getValue();
    } else if (qual != null && qual.getValue() != null) {
      return qual.getValue();
    } else {
      return " other";
    }
  }

}
