package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/** Statistics computed while processing a terminology load. */
@Schema(description = "Statistics computed while processing a terminology load")
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TerminologyStats {

  /** Terminology abbreviation. */
  private String terminology;

  /** Terminology version. */
  private String version;

  /** Number of concepts processed. */
  private long conceptCount;

  /** Number of concepts with a populated code. */
  private long codeCount;

  /** Number of active concepts processed. */
  private long activeConceptCount;

  /** Number of inactive concepts processed. */
  private long inactiveConceptCount;

  /** Number of synonyms processed. */
  private long synonymCount;

  /** Number of definitions processed. */
  private long definitionCount;

  /** Number of properties processed. */
  private long propertyCount;

  /** Number of parent references processed. */
  private long parentReferenceCount;

  /** Number of child references processed. */
  private long childReferenceCount;

  /** Number of associations processed. */
  private long associationCount;

  /** Number of inverse associations processed. */
  private long inverseAssociationCount;

  /** Number of roles processed. */
  private long roleCount;

  /** Number of inverse roles processed. */
  private long inverseRoleCount;

  /** Number of maps processed. */
  private long mapCount;

  /** Hierarchy statistics. */
  private HierarchyStats hierarchy = new HierarchyStats();

  /** Instantiates an empty {@link TerminologyStats}. */
  public TerminologyStats() {
    // n/a
  }

  /**
   * Instantiates a {@link TerminologyStats} from the specified parameters.
   *
   * @param other the other stats
   */
  public TerminologyStats(final TerminologyStats other) {
    populateFrom(other);
  }

  /**
   * Records a processed concept.
   *
   * @param concept the concept
   */
  public void recordConcept(final Concept concept) {
    if (concept == null) {
      return;
    }
    conceptCount++;
    if (StringUtils.isNotBlank(concept.getCode())) {
      codeCount++;
    }
    if (Boolean.TRUE.equals(concept.getActive())) {
      activeConceptCount++;
    } else if (Boolean.FALSE.equals(concept.getActive())) {
      inactiveConceptCount++;
    }
    synonymCount += concept.getSynonyms().size();
    definitionCount += concept.getDefinitions().size();
    propertyCount += concept.getProperties().size();
    parentReferenceCount += concept.getParents().size();
    childReferenceCount += concept.getChildren().size();
    associationCount += concept.getAssociations().size();
    inverseAssociationCount += concept.getInverseAssociations().size();
    roleCount += concept.getRoles().size();
    inverseRoleCount += concept.getInverseRoles().size();
    mapCount += concept.getMaps().size();
  }

  /**
   * Populate from.
   *
   * @param other the other stats
   */
  public void populateFrom(final TerminologyStats other) {
    terminology = other.getTerminology();
    version = other.getVersion();
    conceptCount = other.getConceptCount();
    codeCount = other.getCodeCount();
    activeConceptCount = other.getActiveConceptCount();
    inactiveConceptCount = other.getInactiveConceptCount();
    synonymCount = other.getSynonymCount();
    definitionCount = other.getDefinitionCount();
    propertyCount = other.getPropertyCount();
    parentReferenceCount = other.getParentReferenceCount();
    childReferenceCount = other.getChildReferenceCount();
    associationCount = other.getAssociationCount();
    inverseAssociationCount = other.getInverseAssociationCount();
    roleCount = other.getRoleCount();
    inverseRoleCount = other.getInverseRoleCount();
    mapCount = other.getMapCount();
    hierarchy = new HierarchyStats(other.getHierarchy());
  }

  /**
   * Returns true if no stats have been collected.
   *
   * @return true if no stats have been collected
   */
  @JsonIgnore
  public boolean isEmpty() {
    return conceptCount == 0
        && codeCount == 0
        && activeConceptCount == 0
        && inactiveConceptCount == 0
        && synonymCount == 0
        && definitionCount == 0
        && propertyCount == 0
        && parentReferenceCount == 0
        && childReferenceCount == 0
        && associationCount == 0
        && inverseAssociationCount == 0
        && roleCount == 0
        && inverseRoleCount == 0
        && mapCount == 0
        && (hierarchy == null || hierarchy.isEmpty());
  }

  /**
   * Returns the terminology.
   *
   * @return the terminology
   */
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
   * Returns the concept count.
   *
   * @return the concept count
   */
  public long getConceptCount() {
    return conceptCount;
  }

  /**
   * Sets the concept count.
   *
   * @param conceptCount the concept count
   */
  public void setConceptCount(final long conceptCount) {
    this.conceptCount = conceptCount;
  }

  /**
   * Returns the code count.
   *
   * @return the code count
   */
  public long getCodeCount() {
    return codeCount;
  }

  /**
   * Sets the code count.
   *
   * @param codeCount the code count
   */
  public void setCodeCount(final long codeCount) {
    this.codeCount = codeCount;
  }

  /**
   * Returns the active concept count.
   *
   * @return the active concept count
   */
  public long getActiveConceptCount() {
    return activeConceptCount;
  }

  /**
   * Sets the active concept count.
   *
   * @param activeConceptCount the active concept count
   */
  public void setActiveConceptCount(final long activeConceptCount) {
    this.activeConceptCount = activeConceptCount;
  }

  /**
   * Returns the inactive concept count.
   *
   * @return the inactive concept count
   */
  public long getInactiveConceptCount() {
    return inactiveConceptCount;
  }

  /**
   * Sets the inactive concept count.
   *
   * @param inactiveConceptCount the inactive concept count
   */
  public void setInactiveConceptCount(final long inactiveConceptCount) {
    this.inactiveConceptCount = inactiveConceptCount;
  }

  /**
   * Returns the synonym count.
   *
   * @return the synonym count
   */
  public long getSynonymCount() {
    return synonymCount;
  }

  /**
   * Sets the synonym count.
   *
   * @param synonymCount the synonym count
   */
  public void setSynonymCount(final long synonymCount) {
    this.synonymCount = synonymCount;
  }

  /**
   * Returns the definition count.
   *
   * @return the definition count
   */
  public long getDefinitionCount() {
    return definitionCount;
  }

  /**
   * Sets the definition count.
   *
   * @param definitionCount the definition count
   */
  public void setDefinitionCount(final long definitionCount) {
    this.definitionCount = definitionCount;
  }

  /**
   * Returns the property count.
   *
   * @return the property count
   */
  public long getPropertyCount() {
    return propertyCount;
  }

  /**
   * Sets the property count.
   *
   * @param propertyCount the property count
   */
  public void setPropertyCount(final long propertyCount) {
    this.propertyCount = propertyCount;
  }

  /**
   * Returns the parent reference count.
   *
   * @return the parent reference count
   */
  public long getParentReferenceCount() {
    return parentReferenceCount;
  }

  /**
   * Sets the parent reference count.
   *
   * @param parentReferenceCount the parent reference count
   */
  public void setParentReferenceCount(final long parentReferenceCount) {
    this.parentReferenceCount = parentReferenceCount;
  }

  /**
   * Returns the child reference count.
   *
   * @return the child reference count
   */
  public long getChildReferenceCount() {
    return childReferenceCount;
  }

  /**
   * Sets the child reference count.
   *
   * @param childReferenceCount the child reference count
   */
  public void setChildReferenceCount(final long childReferenceCount) {
    this.childReferenceCount = childReferenceCount;
  }

  /**
   * Returns the association count.
   *
   * @return the association count
   */
  public long getAssociationCount() {
    return associationCount;
  }

  /**
   * Sets the association count.
   *
   * @param associationCount the association count
   */
  public void setAssociationCount(final long associationCount) {
    this.associationCount = associationCount;
  }

  /**
   * Returns the inverse association count.
   *
   * @return the inverse association count
   */
  public long getInverseAssociationCount() {
    return inverseAssociationCount;
  }

  /**
   * Sets the inverse association count.
   *
   * @param inverseAssociationCount the inverse association count
   */
  public void setInverseAssociationCount(final long inverseAssociationCount) {
    this.inverseAssociationCount = inverseAssociationCount;
  }

  /**
   * Returns the role count.
   *
   * @return the role count
   */
  public long getRoleCount() {
    return roleCount;
  }

  /**
   * Sets the role count.
   *
   * @param roleCount the role count
   */
  public void setRoleCount(final long roleCount) {
    this.roleCount = roleCount;
  }

  /**
   * Returns the inverse role count.
   *
   * @return the inverse role count
   */
  public long getInverseRoleCount() {
    return inverseRoleCount;
  }

  /**
   * Sets the inverse role count.
   *
   * @param inverseRoleCount the inverse role count
   */
  public void setInverseRoleCount(final long inverseRoleCount) {
    this.inverseRoleCount = inverseRoleCount;
  }

  /**
   * Returns the map count.
   *
   * @return the map count
   */
  public long getMapCount() {
    return mapCount;
  }

  /**
   * Sets the map count.
   *
   * @param mapCount the map count
   */
  public void setMapCount(final long mapCount) {
    this.mapCount = mapCount;
  }

  /**
   * Returns the hierarchy stats.
   *
   * @return the hierarchy stats
   */
  public HierarchyStats getHierarchy() {
    if (hierarchy == null) {
      hierarchy = new HierarchyStats();
    }
    return hierarchy;
  }

  /**
   * Sets the hierarchy stats.
   *
   * @param hierarchy the hierarchy stats
   */
  public void setHierarchy(final HierarchyStats hierarchy) {
    this.hierarchy = hierarchy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        terminology,
        version,
        conceptCount,
        codeCount,
        activeConceptCount,
        inactiveConceptCount,
        synonymCount,
        definitionCount,
        propertyCount,
        parentReferenceCount,
        childReferenceCount,
        associationCount,
        inverseAssociationCount,
        roleCount,
        inverseRoleCount,
        mapCount,
        hierarchy);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final TerminologyStats other = (TerminologyStats) obj;
    return conceptCount == other.conceptCount
        && codeCount == other.codeCount
        && activeConceptCount == other.activeConceptCount
        && inactiveConceptCount == other.inactiveConceptCount
        && synonymCount == other.synonymCount
        && definitionCount == other.definitionCount
        && propertyCount == other.propertyCount
        && parentReferenceCount == other.parentReferenceCount
        && childReferenceCount == other.childReferenceCount
        && associationCount == other.associationCount
        && inverseAssociationCount == other.inverseAssociationCount
        && roleCount == other.roleCount
        && inverseRoleCount == other.inverseRoleCount
        && mapCount == other.mapCount
        && Objects.equals(terminology, other.terminology)
        && Objects.equals(version, other.version)
        && Objects.equals(hierarchy, other.hierarchy);
  }

  @Override
  public String toString() {
    return "TerminologyStats{"
        + "terminology='"
        + terminology
        + '\''
        + ", version='"
        + version
        + '\''
        + ", conceptCount="
        + conceptCount
        + ", codeCount="
        + codeCount
        + ", activeConceptCount="
        + activeConceptCount
        + ", inactiveConceptCount="
        + inactiveConceptCount
        + ", synonymCount="
        + synonymCount
        + ", definitionCount="
        + definitionCount
        + ", propertyCount="
        + propertyCount
        + ", parentReferenceCount="
        + parentReferenceCount
        + ", childReferenceCount="
        + childReferenceCount
        + ", associationCount="
        + associationCount
        + ", inverseAssociationCount="
        + inverseAssociationCount
        + ", roleCount="
        + roleCount
        + ", inverseRoleCount="
        + inverseRoleCount
        + ", mapCount="
        + mapCount
        + ", hierarchy="
        + hierarchy
        + '}';
  }

  /** Statistics for a code/count pair. */
  @Schema(description = "Statistics for a code/count pair")
  @JsonInclude(Include.NON_EMPTY)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CodeCount {

    /** Concept code. */
    private String code;

    /** Concept name. */
    private String name;

    /** Count value. */
    private long count;

    /** Instantiates an empty {@link CodeCount}. */
    public CodeCount() {
      // n/a
    }

    /**
     * Instantiates a {@link CodeCount} from the specified parameters.
     *
     * @param code the code
     * @param name the name
     * @param count the count
     */
    public CodeCount(final String code, final String name, final long count) {
      this.code = code;
      this.name = name;
      this.count = count;
    }

    /**
     * Instantiates a {@link CodeCount} from the specified parameters.
     *
     * @param other the other code count
     */
    public CodeCount(final CodeCount other) {
      populateFrom(other);
    }

    /**
     * Populate from.
     *
     * @param other the other code count
     */
    public void populateFrom(final CodeCount other) {
      code = other.getCode();
      name = other.getName();
      count = other.getCount();
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
    public void setCode(final String code) {
      this.code = code;
    }

    /**
     * Returns the name.
     *
     * @return the name
     */
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
     * Returns the count.
     *
     * @return the count
     */
    public long getCount() {
      return count;
    }

    /**
     * Sets the count.
     *
     * @param count the count
     */
    public void setCount(final long count) {
      this.count = count;
    }

    @Override
    public int hashCode() {
      return Objects.hash(code, name, count);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      final CodeCount other = (CodeCount) obj;
      return count == other.count
          && Objects.equals(code, other.code)
          && Objects.equals(name, other.name);
    }

    @Override
    public String toString() {
      return "CodeCount{"
          + "code='"
          + code
          + '\''
          + ", name='"
          + name
          + '\''
          + ", count="
          + count
          + '}';
    }
  }

  /** Hierarchy statistics. */
  @Schema(description = "Hierarchy statistics")
  @JsonInclude(Include.NON_EMPTY)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class HierarchyStats {

    /** Indicates whether hierarchy stats apply to the terminology. */
    private Boolean applicable;

    /** Number of codes present in the hierarchy path map. */
    private long codeCount;

    /** Number of tree positions across all paths. */
    private long treePositionCount;

    /** Number of hierarchy roots. */
    private long rootCount;

    /** Code with the minimum number of paths. */
    private CodeCount minPaths;

    /** Code with the maximum number of paths. */
    private CodeCount maxPaths;

    /** Code with the maximum number of children. */
    private CodeCount maxChildren;

    /** Code with the maximum number of parents. */
    private CodeCount maxParents;

    /** Instantiates an empty {@link HierarchyStats}. */
    public HierarchyStats() {
      // n/a
    }

    /**
     * Instantiates a {@link HierarchyStats} from the specified parameters.
     *
     * @param other the other hierarchy stats
     */
    public HierarchyStats(final HierarchyStats other) {
      populateFrom(other);
    }

    /**
     * Populate from.
     *
     * @param other the other hierarchy stats
     */
    public void populateFrom(final HierarchyStats other) {
      applicable = other.getApplicable();
      codeCount = other.getCodeCount();
      treePositionCount = other.getTreePositionCount();
      rootCount = other.getRootCount();
      minPaths = other.getMinPaths() == null ? null : new CodeCount(other.getMinPaths());
      maxPaths = other.getMaxPaths() == null ? null : new CodeCount(other.getMaxPaths());
      maxChildren = other.getMaxChildren() == null ? null : new CodeCount(other.getMaxChildren());
      maxParents = other.getMaxParents() == null ? null : new CodeCount(other.getMaxParents());
    }

    /**
     * Returns true if no hierarchy stats have been collected.
     *
     * @return true if no hierarchy stats have been collected
     */
    @JsonIgnore
    public boolean isEmpty() {
      return applicable == null
          && codeCount == 0
          && treePositionCount == 0
          && rootCount == 0
          && minPaths == null
          && maxPaths == null
          && maxChildren == null
          && maxParents == null;
    }

    /**
     * Returns the applicable flag.
     *
     * @return the applicable flag
     */
    public Boolean getApplicable() {
      return applicable;
    }

    /**
     * Sets the applicable flag.
     *
     * @param applicable the applicable flag
     */
    public void setApplicable(final Boolean applicable) {
      this.applicable = applicable;
    }

    /**
     * Returns the hierarchy code count.
     *
     * @return the hierarchy code count
     */
    public long getCodeCount() {
      return codeCount;
    }

    /**
     * Sets the hierarchy code count.
     *
     * @param codeCount the hierarchy code count
     */
    public void setCodeCount(final long codeCount) {
      this.codeCount = codeCount;
    }

    /**
     * Returns the tree position count.
     *
     * @return the tree position count
     */
    public long getTreePositionCount() {
      return treePositionCount;
    }

    /**
     * Sets the tree position count.
     *
     * @param treePositionCount the tree position count
     */
    public void setTreePositionCount(final long treePositionCount) {
      this.treePositionCount = treePositionCount;
    }

    /**
     * Returns the root count.
     *
     * @return the root count
     */
    public long getRootCount() {
      return rootCount;
    }

    /**
     * Sets the root count.
     *
     * @param rootCount the root count
     */
    public void setRootCount(final long rootCount) {
      this.rootCount = rootCount;
    }

    /**
     * Returns the code with the minimum number of paths.
     *
     * @return the code with the minimum number of paths
     */
    public CodeCount getMinPaths() {
      return minPaths;
    }

    /**
     * Sets the code with the minimum number of paths.
     *
     * @param minPaths the code with the minimum number of paths
     */
    public void setMinPaths(final CodeCount minPaths) {
      this.minPaths = minPaths;
    }

    /**
     * Returns the code with the maximum number of paths.
     *
     * @return the code with the maximum number of paths
     */
    public CodeCount getMaxPaths() {
      return maxPaths;
    }

    /**
     * Sets the code with the maximum number of paths.
     *
     * @param maxPaths the code with the maximum number of paths
     */
    public void setMaxPaths(final CodeCount maxPaths) {
      this.maxPaths = maxPaths;
    }

    /**
     * Returns the code with the maximum number of children.
     *
     * @return the code with the maximum number of children
     */
    public CodeCount getMaxChildren() {
      return maxChildren;
    }

    /**
     * Sets the code with the maximum number of children.
     *
     * @param maxChildren the code with the maximum number of children
     */
    public void setMaxChildren(final CodeCount maxChildren) {
      this.maxChildren = maxChildren;
    }

    /**
     * Returns the code with the maximum number of parents.
     *
     * @return the code with the maximum number of parents
     */
    public CodeCount getMaxParents() {
      return maxParents;
    }

    /**
     * Sets the code with the maximum number of parents.
     *
     * @param maxParents the code with the maximum number of parents
     */
    public void setMaxParents(final CodeCount maxParents) {
      this.maxParents = maxParents;
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          applicable,
          codeCount,
          treePositionCount,
          rootCount,
          minPaths,
          maxPaths,
          maxChildren,
          maxParents);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      final HierarchyStats other = (HierarchyStats) obj;
      return codeCount == other.codeCount
          && treePositionCount == other.treePositionCount
          && rootCount == other.rootCount
          && Objects.equals(applicable, other.applicable)
          && Objects.equals(minPaths, other.minPaths)
          && Objects.equals(maxPaths, other.maxPaths)
          && Objects.equals(maxChildren, other.maxChildren)
          && Objects.equals(maxParents, other.maxParents);
    }

    @Override
    public String toString() {
      return "HierarchyStats{"
          + "applicable="
          + applicable
          + ", codeCount="
          + codeCount
          + ", treePositionCount="
          + treePositionCount
          + ", rootCount="
          + rootCount
          + ", minPaths="
          + minPaths
          + ", maxPaths="
          + maxPaths
          + ", maxChildren="
          + maxChildren
          + ", maxParents="
          + maxParents
          + '}';
    }
  }
}
