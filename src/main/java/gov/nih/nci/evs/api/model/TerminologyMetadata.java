package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents terminology metadata, which includes information about mapping from OWL structures to
 * the terminology model.
 */
@Schema(description = "Represents additional terminology metadata")
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

  /** The excluded properties. */
  private Set<String> excludedProperties;

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

  /** The prefix. */
  private String sparqlPrefix;

  /** The hierarchy flag. */
  private Boolean hierarchy;

  /** The hierarchy roles. */
  private Set<String> hierarchyRoles;

  /** The history flag. */
  private Boolean history;

  /** The mapsets flag. */
  private Boolean mapsets;

  /** The source ct. */
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

  /** The extra subsets. */
  private Map<String, String> extraSubsets;

  /** Instantiates an empty {@link TerminologyMetadata}. */
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
    excludedProperties = new HashSet<>(other.getExcludedProperties());
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
    sparqlPrefix = other.getSparqlPrefix();
    sourceCt = sources.size();
    hierarchy = other.getHierarchy();
    hierarchyRoles = new HashSet<>(other.getHierarchyRoles());
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
    licenseCheck = other.getLicenseCheck();
    licenseText = other.getLicenseText();
    metaConceptField = other.getMetaConceptField();
    preferredTermTypes = new ArrayList<>(other.getPreferredTermTypes());
    subset = new HashSet<>(other.getSubset());
    codeLabel = other.getCodeLabel();
    welcomeText = other.getWelcomeText();
    extraSubsets = new HashMap<>(other.getExtraSubsets());
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((uiLabel == null) ? 0 : uiLabel.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    final TerminologyMetadata other = (TerminologyMetadata) obj;
    if (uiLabel == null) {
      if (other.uiLabel != null) {
        return false;
      }
    } else if (!uiLabel.equals(other.uiLabel)) {
      return false;
    }
    return true;
  }

  /**
   * Returns the relationship to target.
   *
   * @return the relationship to target
   */
  @Schema(hidden = true)
  public String getRelationshipToTarget() {
    return relationshipToTarget;
  }

  /**
   * Sets the relationship to target.
   *
   * @param relationshipToTarget the relationship to target
   */
  public void setRelationshipToTarget(final String relationshipToTarget) {
    this.relationshipToTarget = relationshipToTarget;
  }

  /**
   * Returns the ui label.
   *
   * @return the ui label
   */
  @Schema(description = "Human-readable name for the terminology to use in a UI label")
  public String getUiLabel() {
    return uiLabel;
  }

  /**
   * Sets the ui label.
   *
   * @param uiLabel the ui label
   */
  public void setUiLabel(final String uiLabel) {
    this.uiLabel = uiLabel;
  }

  /**
   * Returns the max versions.
   *
   * @return the max versions
   */
  @Schema(description = "Max number of versions to keep at the same time")
  public Integer getMaxVersions() {
    return maxVersions;
  }

  /**
   * Sets the max versions.
   *
   * @param maxVersions the max versions
   */
  public void setMaxVersions(final Integer maxVersions) {
    this.maxVersions = maxVersions;
  }

  /**
   * Returns the loader.
   *
   * @return the loader
   */
  @Schema(description = "Label for the loader used to populate this data")
  public String getLoader() {
    return loader;
  }

  /**
   * Sets the loader.
   *
   * @param loader the loader
   */
  public void setLoader(final String loader) {
    this.loader = loader;
  }

  /**
   * Returns the code.
   *
   * @return the code
   */
  @Schema(hidden = true)
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
   * Returns the concept status.
   *
   * @return the concept status
   */
  @Schema(hidden = true)
  public String getConceptStatus() {
    return conceptStatus;
  }

  /**
   * Sets the concept status.
   *
   * @param conceptStatus the concept status
   */
  public void setConceptStatus(final String conceptStatus) {
    this.conceptStatus = conceptStatus;
  }

  /**
   * Returns the concept statuses.
   *
   * @return the concept statuses
   */
  @Schema(description = "Concept status values used by the terminology")
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
  public void setConceptStatuses(final List<String> conceptStatuses) {
    this.conceptStatuses = conceptStatuses;
  }

  /**
   * Returns the excluded properties.
   *
   * @return the excludeProperties
   */
  public Set<String> getExcludedProperties() {
    if (excludedProperties == null) {
      excludedProperties = new HashSet<>();
    }
    return excludedProperties;
  }

  /**
   * Sets the excluded properties.
   *
   * @param excludeProperties the excludeProperties to set
   */
  public void setExcludedProperties(final Set<String> excludeProperties) {
    this.excludedProperties = excludeProperties;
  }

  /**
   * Returns the retired status value.
   *
   * @return the retired status value
   */
  @Schema(description = "Concept status value for retired concepts")
  public String getRetiredStatusValue() {
    return retiredStatusValue;
  }

  /**
   * Sets the retired status value.
   *
   * @param retiredStatusValue the retired status value
   */
  public void setRetiredStatusValue(final String retiredStatusValue) {
    this.retiredStatusValue = retiredStatusValue;
  }

  /**
   * Returns the preferred name.
   *
   * @return the preferred name
   */
  @Schema(hidden = true)
  public String getPreferredName() {
    return preferredName;
  }

  /**
   * Sets the preferred name.
   *
   * @param preferredName the preferred name
   */
  public void setPreferredName(final String preferredName) {
    this.preferredName = preferredName;
  }

  /**
   * Returns the synonym.
   *
   * @return the synonym
   */
  @Schema(hidden = true)
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
  public void setSynonym(final Set<String> synonym) {
    this.synonym = synonym;
  }

  /**
   * Returns the synonym term type.
   *
   * @return the synonym term type
   */
  @Schema(hidden = true)
  public String getSynonymTermType() {
    return synonymTermType;
  }

  /**
   * Sets the synonym term type.
   *
   * @param synonymTermType the synonym term type
   */
  public void setSynonymTermType(final String synonymTermType) {
    this.synonymTermType = synonymTermType;
  }

  /**
   * Sets the synonym term group. This is a bridge to support naming convention normalization.
   *
   * @param synonymTermGroup the synonym term group
   */
  public void setSynonymTermGroup(final String synonymTermGroup) {
    this.synonymTermType = synonymTermGroup;
  }

  /**
   * Returns the synonym source.
   *
   * @return the synonym source
   */
  @Schema(hidden = true)
  public String getSynonymSource() {
    return synonymSource;
  }

  /**
   * Sets the synonym source.
   *
   * @param synonymSource the synonym source
   */
  public void setSynonymSource(final String synonymSource) {
    this.synonymSource = synonymSource;
  }

  /**
   * Returns the synonym code.
   *
   * @return the synonym code
   */
  @Schema(hidden = true)
  public String getSynonymCode() {
    return synonymCode;
  }

  /**
   * Sets the synonym code.
   *
   * @param synonymCode the synonym code
   */
  public void setSynonymCode(final String synonymCode) {
    this.synonymCode = synonymCode;
  }

  /**
   * Returns the synonym sub source.
   *
   * @return the synonym sub source
   */
  @Schema(hidden = true)
  public String getSynonymSubSource() {
    return synonymSubSource;
  }

  /**
   * Sets the synonym sub source.
   *
   * @param synonymSubSource the synonym sub source
   */
  public void setSynonymSubSource(final String synonymSubSource) {
    this.synonymSubSource = synonymSubSource;
  }

  /**
   * Returns the definition source.
   *
   * @return the definition source
   */
  @Schema(hidden = true)
  public String getDefinitionSource() {
    return definitionSource;
  }

  /**
   * Sets the definition source.
   *
   * @param definitionSource the definition source
   */
  public void setDefinitionSource(final String definitionSource) {
    this.definitionSource = definitionSource;
  }

  /**
   * Returns the definition.
   *
   * @return the definition
   */
  @Schema(hidden = true)
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
  public void setDefinition(final Set<String> definition) {
    this.definition = definition;
  }

  /**
   * Returns the map relation.
   *
   * @return the map relation
   */
  @Schema(hidden = true)
  public String getMapRelation() {
    return mapRelation;
  }

  /**
   * Sets the map relation.
   *
   * @param mapRelation the map relation
   */
  public void setMapRelation(final String mapRelation) {
    this.mapRelation = mapRelation;
  }

  /**
   * Returns the map.
   *
   * @return the map
   */
  @Schema(hidden = true)
  public String getMap() {
    return map;
  }

  /**
   * Sets the map.
   *
   * @param map the map
   */
  public void setMap(final String map) {
    this.map = map;
  }

  /**
   * Returns the map target.
   *
   * @return the map target
   */
  @Schema(hidden = true)
  public String getMapTarget() {
    return mapTarget;
  }

  /**
   * Sets the map target.
   *
   * @param mapTarget the map target
   */
  public void setMapTarget(final String mapTarget) {
    this.mapTarget = mapTarget;
  }

  /**
   * Returns the map target term type.
   *
   * @return the map target term type
   */
  @Schema(hidden = true)
  public String getMapTargetTermType() {
    return mapTargetTermType;
  }

  /**
   * Sets the map target term type.
   *
   * @param mapTargetTermType the map target term type
   */
  public void setMapTargetTermType(final String mapTargetTermType) {
    this.mapTargetTermType = mapTargetTermType;
  }

  /**
   * Sets the map target term group. This is a bridge to support naming convention normalization.
   *
   * @param mapTargetTermGroup the map target term group
   */
  @Schema(hidden = true)
  public void setMapTargetTermGroup(final String mapTargetTermGroup) {
    this.mapTargetTermType = mapTargetTermGroup;
  }

  /**
   * Returns the map target terminology.
   *
   * @return the map target terminology
   */
  @Schema(hidden = true)
  public String getMapTargetTerminology() {
    return mapTargetTerminology;
  }

  /**
   * Sets the map target terminology.
   *
   * @param mapTargetTerminology the map target terminology
   */
  public void setMapTargetTerminology(final String mapTargetTerminology) {
    this.mapTargetTerminology = mapTargetTerminology;
  }

  /**
   * Returns the map target terminology version.
   *
   * @return the map target terminology version
   */
  @Schema(hidden = true)
  public String getMapTargetTerminologyVersion() {
    return mapTargetTerminologyVersion;
  }

  /**
   * Sets the map target terminology version.
   *
   * @param mapTargetTerminologyVersion the map target terminology version
   */
  public void setMapTargetTerminologyVersion(final String mapTargetTerminologyVersion) {
    this.mapTargetTerminologyVersion = mapTargetTerminologyVersion;
  }

  /**
   * Returns the sources.
   *
   * @return the sources
   */
  @Schema(hidden = true)
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
  @Schema(description = "Count of included sources")
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
  public void setSources(final Map<String, String> sources) {
    this.sources = sources;
  }

  /**
   * Returns the details columns.
   *
   * @return the details columns
   */
  @Schema(description = "Metadata for displaying concept")
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
  public void setDetailsColumns(final Map<String, Boolean> detailsColumns) {
    this.detailsColumns = detailsColumns;
  }

  /**
   * Returns the definition source set.
   *
   * @return the definition source set
   */
  @Schema(hidden = true)
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
  public void setDefinitionSourceSet(final Set<String> definitionSourceSet) {
    this.definitionSourceSet = definitionSourceSet;
  }

  /**
   * Returns the synonym source set.
   *
   * @return the synonym source set
   */
  @Schema(hidden = true)
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
  public void setSynonymSourceSet(final Set<String> synonymSourceSet) {
    this.synonymSourceSet = synonymSourceSet;
  }

  /**
   * Returns the term types.
   *
   * @return the term types
   */
  @Schema(hidden = true)
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
  public void setTermTypes(final Map<String, String> termTypes) {
    this.termTypes = termTypes;
  }

  /**
   * Sets the term groups. This is a bridge to support naming convention normalization.
   *
   * @param termGroups the term groups
   */
  public void setTermGroups(final Map<String, String> termGroups) {
    this.termTypes = termGroups;
  }

  /**
   * Returns the subset link property.
   *
   * @return the subset link property
   */
  @Schema(description = "Metadata for downloading a subset")
  public String getSubsetLink() {
    return subsetLink;
  }

  /**
   * Sets the subset link property.
   *
   * @param subsetLink the subset link property
   */
  public void setSubsetLink(final String subsetLink) {
    this.subsetLink = subsetLink;
  }

  /**
   * Returns the subset prefix.
   *
   * @return the subsetPrefix
   */
  @Schema(hidden = true)
  public String getSubsetPrefix() {
    return subsetPrefix;
  }

  /**
   * Sets the subset prefix.
   *
   * @param subsetPrefix the subsetPrefix to set
   */
  public void setSubsetPrefix(final String subsetPrefix) {
    this.subsetPrefix = subsetPrefix;
  }

  /**
   * Returns the sparql prefix.
   *
   * @return the sparql prefix
   */
  @Schema(hidden = true)
  public String getSparqlPrefix() {
    return sparqlPrefix;
  }

  /**
   * Sets the sparql prefix.
   *
   * @param sparqlPrefix the sparql prefix
   */
  public void setSparqlPrefix(final String sparqlPrefix) {
    this.sparqlPrefix = sparqlPrefix;
  }

  /**
   * Returns the sources to remove.
   *
   * @return the sources to remove
   */
  @Schema(hidden = true)
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
  public void setSourcesToRemove(final Set<String> sourcesToRemove) {
    this.sourcesToRemove = sourcesToRemove;
  }

  /**
   * Returns the subset member.
   *
   * @return the subsetMember
   */
  @Schema(hidden = true)
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
  public void setSubsetMember(final Set<String> subsetMember) {
    this.subsetMember = subsetMember;
  }

  /**
   * Returns the unpublished properties or qualifiers. These are things where the data defines the
   * metadata but then no uses of that metadata occur.
   *
   * @return the unpublished
   */
  @Schema(hidden = true)
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
  public void setUnpublished(final Set<String> unpublished) {
    this.unpublished = unpublished;
  }

  /**
   * Returns the monthly db.
   *
   * @return the monthly db
   */
  @Schema(hidden = true)
  public String getMonthlyDb() {
    return monthlyDb;
  }

  /**
   * Sets the monthly db.
   *
   * @param monthlyDb the monthly db
   */
  public void setMonthlyDb(final String monthlyDb) {
    this.monthlyDb = monthlyDb;
  }

  /**
   * Returns the license text.
   *
   * @return the license text
   */
  @Schema(description = "License text for the UI")
  public String getLicenseText() {
    return licenseText;
  }

  /**
   * Sets the license text.
   *
   * @param licenseText the license text
   */
  public void setLicenseText(final String licenseText) {
    this.licenseText = licenseText;
  }

  /**
   * Returns the license check.
   *
   * @return the license check
   */
  @Schema(hidden = true)
  public String getLicenseCheck() {
    return licenseCheck;
  }

  /**
   * Sets the license check.
   *
   * @param licenseCheck the license check
   */
  public void setLicenseCheck(final String licenseCheck) {
    this.licenseCheck = licenseCheck;
  }

  /**
   * Returns the meta concept field.
   *
   * @return the meta concept field
   */
  @Schema(description = "Metadata for concept display")
  public String getMetaConceptField() {
    return metaConceptField;
  }

  /**
   * Sets the meta concept field.
   *
   * @param metaConceptField the meta concept field
   */
  public void setMetaConceptField(final String metaConceptField) {
    this.metaConceptField = metaConceptField;
  }

  /**
   * Returns the preferred term types.
   *
   * @return the preferred term types
   */
  @Schema(hidden = true)
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
  public void setPreferredTermTypes(final List<String> preferredTermTypes) {
    this.preferredTermTypes = preferredTermTypes;
  }

  /**
   * Returns the code label.
   *
   * @return the codeLabel
   */
  @Schema(description = "Metadata for concept display")
  public String getCodeLabel() {
    return codeLabel;
  }

  /**
   * Sets the code label.
   *
   * @param codeLabel the codeLabel to set
   */
  public void setCodeLabel(final String codeLabel) {
    this.codeLabel = codeLabel;
  }

  /**
   * Returns the welcome text.
   *
   * @return the welcomeText
   */
  @Schema(description = "Metadata for landing page welcome text")
  public String getWelcomeText() {
    return welcomeText;
  }

  /**
   * Sets the welcome text.
   *
   * @param welcomeText the welcomeText to set
   */
  public void setWelcomeText(final String welcomeText) {
    this.welcomeText = welcomeText;
  }

  /**
   * Returns the extra subsets.
   *
   * @return the extra subsets
   */
  @Schema(hidden = true)
  public Map<String, String> getExtraSubsets() {
    if (extraSubsets == null) {
      extraSubsets = new HashMap<>();
    }
    return extraSubsets;
  }

  /**
   * Sets the extra subsets.
   *
   * @param extraSubsets the extra subsets
   */
  public void setExtraSubsets(final Map<String, String> extraSubsets) {
    this.extraSubsets = extraSubsets;
  }

  /**
   * Sets the preferred term groups. This is a bridge to support naming convention normalization.
   *
   * @param preferredTermGroups the preferred term groups
   */
  public void setPreferredTermGroups(final List<String> preferredTermGroups) {
    this.preferredTermTypes = preferredTermGroups;
  }

  /**
   * Returns the subset.
   *
   * @return the subset
   */
  @Schema(hidden = true)
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
  public void setSubset(final Set<String> subset) {
    this.subset = subset;
  }

  /**
   * Returns the hierarchy.
   *
   * @return the hierarchy
   */
  @Schema(description = "Indicates whether or not the terminology has a hierarchy")
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
  public void setHierarchy(final Boolean hierarchy) {
    this.hierarchy = hierarchy;
  }

  /**
   * Returns the hierarchy roles.
   *
   * @return the hierarchy
   */
  @Schema(description = "Indicates role codes that are reinterpreted as parent/child")
  public Set<String> getHierarchyRoles() {
    if (hierarchyRoles == null) {
      hierarchyRoles = new HashSet<>();
    }
    return hierarchyRoles;
  }

  /**
   * Sets the hierarchy roles.
   *
   * @param hierarchyRoles the hierarchyRoles to set
   */
  public void setHierarchyRoles(final Set<String> hierarchyRoles) {
    this.hierarchyRoles = hierarchyRoles;
  }

  /**
   * Returns the history.
   *
   * @return the history
   */
  @Schema(description = "Indicates whether or not the terminology has history records")
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
  public void setHistory(final Boolean history) {
    this.history = history;
  }

  /**
   * Returns the mapsets.
   *
   * @return the mapsets
   */
  @Schema(hidden = true)
  public Boolean getMapsets() {
    return mapsets;
  }

  /**
   * Sets the mapsets.
   *
   * @param mapsets the mapsets to set
   */
  public void setMapsets(final Boolean mapsets) {
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
    return getSynonym().contains(code)
        || getDefinition().contains(code)
        || code.equals(this.code)
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
    return code.equals(synonymTermType)
        || code.equals(synonymSource)
        || code.equals(synonymCode)
        || code.equals(synonymSubSource)
        || code.equals(definitionSource)
        || code.equals(mapRelation)
        || code.equals(mapTarget)
        || code.equals(mapTargetTermType)
        || code.equals(mapTargetTerminology)
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
  public String getRemodeledAsType(
      final Property prop, final Qualifier qual, final TerminologyMetadata md) {
    String code = null;
    if (prop != null) {
      code = prop.getCode();
    } else if (qual != null) {
      code = qual.getCode();
    } else {
      return " other";
    }

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
