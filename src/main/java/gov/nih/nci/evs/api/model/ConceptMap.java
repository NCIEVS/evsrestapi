package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

/** Represents a map to a concept in another terminology. */
@Schema(description = "Represents a map from a concept to concepts in other terminologies")
@JsonInclude(Include.NON_EMPTY)
public class ConceptMap extends BaseModel implements Comparable<ConceptMap> {

  /** The mapset code */
  @Field(type = FieldType.Keyword)
  private String mapsetCode;

  /** The source. */
  @Field(type = FieldType.Keyword)
  private String source;

  /** The source name. */
  @MultiField(
      mainField = @Field(type = FieldType.Text),
      otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)})
  private String sourceName;

  /** The source term type. */
  @Field(type = FieldType.Keyword)
  private String sourceTermType;

  /** The source code. */
  @Field(type = FieldType.Keyword)
  private String sourceCode;

  /** The source terminology. */
  @Field(type = FieldType.Keyword)
  private String sourceTerminology;

  /** The source terminology version. */
  @Field(type = FieldType.Keyword)
  private String sourceTerminologyVersion;

  /** Is source terminology loaded */
  @Field(type = FieldType.Keyword)
  private Boolean sourceLoaded;

  /** The type. */
  @Field(type = FieldType.Keyword)
  private String type;

  /** The rank. */
  @Field(type = FieldType.Keyword)
  private String rank;

  /** The group. */
  @Field(type = FieldType.Keyword)
  private String group;

  /** The rule. */
  @Field(type = FieldType.Keyword)
  private String rule;

  /** The target name. */
  @MultiField(
      mainField = @Field(type = FieldType.Text),
      otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)})
  private String targetName;

  /** The target term type. */
  @Field(type = FieldType.Keyword)
  private String targetTermType;

  /** The target code. */
  @Field(type = FieldType.Keyword)
  private String targetCode;

  /** The target terminology. */
  @Field(type = FieldType.Keyword)
  private String targetTerminology;

  /** The target terminology version. */
  @Field(type = FieldType.Keyword)
  private String targetTerminologyVersion;

  /** Is target terminology loaded */
  @Field(type = FieldType.Keyword)
  private Boolean targetLoaded;

  /** The mapset code */
  @Field(type = FieldType.Keyword)
  private String sortKey;

  /** Instantiates an empty {@link ConceptMap}. */
  public ConceptMap() {
    // n/a
  }

  /**
   * Instantiates a {@link ConceptMap} from the specified parameters.
   *
   * @param other the other
   */
  public ConceptMap(final ConceptMap other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final ConceptMap other) {
    super.populateFrom(other);
    mapsetCode = other.getMapsetCode();
    source = other.getSource();
    type = other.getType();
    group = other.getGroup();
    rank = other.getRank();
    rule = other.getRule();
    targetName = other.getTargetName();
    targetTermType = other.getTargetTermType();
    sourceCode = other.getSourceCode();
    sourceName = other.getSourceName();
    sourceTermType = other.getSourceTermType();
    sourceTerminology = other.getSourceTerminology();
    sourceTerminologyVersion = other.getSourceTerminologyVersion();
    sourceLoaded = other.getSourceLoaded();
    targetCode = other.getTargetCode();
    targetName = other.getTargetName();
    targetTerminology = other.getTargetTerminology();
    targetTerminologyVersion = other.getTargetTerminologyVersion();
    targetLoaded = other.getTargetLoaded();
    sortKey = other.getSortKey();
  }

  /**
   * @return the mapsetCode
   */
  public String getMapsetCode() {
    return mapsetCode;
  }

  /**
   * @param mapsetCode the mapsetCode to set
   */
  public void setMapsetCode(String mapsetCode) {
    this.mapsetCode = mapsetCode;
  }

  /**
   * Returns the source.
   *
   * @return the source
   */
  @Schema(description = "Source terminology of the map")
  public String getSource() {
    return source;
  }

  /**
   * Sets the source.
   *
   * @param source the source to set
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * Returns the source name.
   *
   * @return the sourceName
   */
  @Schema(description = "Preferred name of the source code")
  public String getSourceName() {
    return sourceName;
  }

  /**
   * Sets the source name.
   *
   * @param sourceName the sourceName to set
   */
  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  /**
   * Returns the source term type.
   *
   * @return the sourceTermType
   */
  @Schema(description = "Term type of the source code")
  public String getSourceTermType() {
    return sourceTermType;
  }

  /**
   * Sets the source term type.
   *
   * @param sourceTermType the sourceTermType to set
   */
  public void setSourceTermType(String sourceTermType) {
    this.sourceTermType = sourceTermType;
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
  public void setType(final String type) {
    this.type = type;
  }

  /**
   * Returns the group.
   *
   * @return the group
   */
  public String getGroup() {
    return group;
  }

  /**
   * Sets the group.
   *
   * @param group the group
   */
  public void setGroup(final String group) {
    this.group = group;
  }

  /**
   * Returns the rank.
   *
   * @return the rank
   */
  public String getRank() {
    return rank;
  }

  /**
   * Sets the rank.
   *
   * @param rank the rank
   */
  public void setRank(final String rank) {
    this.rank = rank;
  }

  /**
   * Returns the rule.
   *
   * @return the rule
   */
  public String getRule() {
    return rule;
  }

  /**
   * Sets the rule.
   *
   * @param rule the rule
   */
  public void setRule(final String rule) {
    this.rule = rule;
  }

  /**
   * Returns the target name.
   *
   * @return the target name
   */
  public String getTargetName() {
    return targetName;
  }

  /**
   * Sets the target name.
   *
   * @param targetName the target name
   */
  public void setTargetName(final String targetName) {
    this.targetName = targetName;
  }

  /**
   * Returns the target term type.
   *
   * @return the target term type
   */
  public String getTargetTermType() {
    return targetTermType;
  }

  /**
   * Sets the target term type.
   *
   * @param targetTermType the target term type
   */
  public void setTargetTermType(final String targetTermType) {
    this.targetTermType = targetTermType;
  }

  /**
   * Sets the target term group. This is a bridge to support naming convention normalization.
   *
   * @param targetTermGroup the target term group
   */
  public void setTargetTermGroup(final String targetTermGroup) {
    this.targetTermType = targetTermGroup;
  }

  /**
   * Returns the target code.
   *
   * @return the target code
   */
  @Schema(description = "Target code of the map (the thing being mapped 'to')")
  public String getTargetCode() {
    return targetCode;
  }

  /**
   * Sets the target code.
   *
   * @param targetCode the target code
   */
  public void setTargetCode(final String targetCode) {
    this.targetCode = targetCode;
  }

  /**
   * Returns the source code.
   *
   * @return the source code
   */
  @Schema(description = "Source code of the map (the thing being mapped 'from')")
  public String getSourceCode() {
    return sourceCode;
  }

  /**
   * Sets the source code.
   *
   * @param sourceCode the source code
   */
  public void setSourceCode(final String sourceCode) {
    this.sourceCode = sourceCode;
  }

  /**
   * Returns the source terminology.
   *
   * @return the source terminology
   */
  @Schema(description = "Terminology of the source code")
  public String getSourceTerminology() {
    return sourceTerminology;
  }

  /**
   * Sets the source terminology.
   *
   * @param sourceTerminology the source terminology
   */
  public void setSourceTerminology(final String sourceTerminology) {
    this.sourceTerminology = sourceTerminology;
  }

  /**
   * @return the sourceTerminologyVersion
   */
  @Schema(description = "Terminology version of the source code")
  public String getSourceTerminologyVersion() {
    return sourceTerminologyVersion;
  }

  /**
   * @param sourceTerminologyVersion the sourceTerminologyVersion to set
   */
  public void setSourceTerminologyVersion(String sourceTerminologyVersion) {
    this.sourceTerminologyVersion = sourceTerminologyVersion;
  }

  /**
   * @return the sourceLoaded
   */
  @Schema(description = "Indicates whether the source code terminology/version is currently loaded")
  public Boolean getSourceLoaded() {
    return sourceLoaded;
  }

  /**
   * @param sourceLoaded the sourceLoaded to set
   */
  public void setSourceLoaded(Boolean sourceLoaded) {
    this.sourceLoaded = sourceLoaded;
  }

  /**
   * Returns the target terminology.
   *
   * @return the target terminology
   */
  @Schema(description = "Terminology of the target code")
  public String getTargetTerminology() {
    return targetTerminology;
  }

  /**
   * Sets the target terminology.
   *
   * @param targetTerminology the target terminology
   */
  public void setTargetTerminology(final String targetTerminology) {
    this.targetTerminology = targetTerminology;
  }

  /**
   * Returns the target terminology version.
   *
   * @return the target terminology version
   */
  @Schema(description = "Terminology version of the target code")
  public String getTargetTerminologyVersion() {
    return targetTerminologyVersion;
  }

  /**
   * Sets the target terminology version.
   *
   * @param targetTerminologyVersion the target terminology version
   */
  public void setTargetTerminologyVersion(final String targetTerminologyVersion) {
    this.targetTerminologyVersion = targetTerminologyVersion;
  }

  /**
   * @return the targetLoaded
   */
  @Schema(description = "Indicates whether the target terminology/version is currently loaded")
  public Boolean getTargetLoaded() {
    return targetLoaded;
  }

  /**
   * @param targetLoaded the targetLoaded to set
   */
  public void setTargetLoaded(Boolean targetLoaded) {
    this.targetLoaded = targetLoaded;
  }

  /**
   * @return the sortKey
   */
  public String getSortKey() {
    return sortKey;
  }

  /**
   * @param sortKey the sortKey to set
   */
  public void setSortKey(String sortKey) {
    this.sortKey = sortKey;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    return Objects.hash(
        mapsetCode,
        group,
        rank,
        rule,
        source,
        sourceCode,
        sourceName,
        sourceTermType,
        sourceTerminology,
        sourceTerminologyVersion,
        sourceLoaded,
        targetCode,
        targetName,
        targetTermType,
        targetTerminology,
        targetTerminologyVersion,
        targetLoaded,
        type);
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ConceptMap other = (ConceptMap) obj;
    return Objects.equals(mapsetCode, other.mapsetCode)
        && Objects.equals(group, other.group)
        && Objects.equals(rank, other.rank)
        && Objects.equals(rule, other.rule)
        && Objects.equals(source, other.source)
        && Objects.equals(sourceCode, other.sourceCode)
        && Objects.equals(sourceName, other.sourceName)
        && Objects.equals(sourceTermType, other.sourceTermType)
        && Objects.equals(sourceTerminology, other.sourceTerminology)
        && Objects.equals(sourceTerminologyVersion, other.sourceTerminologyVersion)
        && Objects.equals(sourceLoaded, other.sourceLoaded)
        && Objects.equals(targetCode, other.targetCode)
        && Objects.equals(targetName, other.targetName)
        && Objects.equals(targetTermType, other.targetTermType)
        && Objects.equals(targetTerminology, other.targetTerminology)
        && Objects.equals(targetTerminologyVersion, other.targetTerminologyVersion)
        && Objects.equals(targetLoaded, other.targetLoaded)
        && Objects.equals(type, other.type);
  }

  /* see superclass */
  @Override
  public int compareTo(ConceptMap o) {
    return (sourceCode + sourceTerminology + group + rank + targetName + targetCode)
        .compareToIgnoreCase(
            o.getSourceCode()
                + o.getSourceTerminology()
                + o.getGroup()
                + o.getRank()
                + o.getTargetName()
                + o.getTargetCode());
  }
}
