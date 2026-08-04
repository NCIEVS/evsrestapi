package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyStats;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import org.apache.commons.lang3.Strings;

/** Collects terminology statistics during a load. */
public class TerminologyStatsCollector {

  /** The load statistics. */
  private TerminologyStats statistics = new TerminologyStats();

  /**
   * Returns the load statistics.
   *
   * @return the load statistics
   */
  public TerminologyStats getStatistics() {
    return statistics;
  }

  /**
   * Resets the load statistics.
   *
   * @param terminology the terminology
   */
  public void reset(final Terminology terminology) {
    statistics = new TerminologyStats();
    if (terminology != null) {
      statistics.setTerminology(terminology.getTerminology());
      statistics.setVersion(terminology.getVersion());
    }
  }

  /**
   * Ensures load statistics are initialized for the terminology.
   *
   * @param terminology the terminology
   */
  public void ensure(final Terminology terminology) {
    if (statistics == null
        || terminology != null
            && (!Strings.CS.equals(statistics.getTerminology(), terminology.getTerminology())
                || !Strings.CS.equals(statistics.getVersion(), terminology.getVersion()))) {
      reset(terminology);
    }
  }

  /**
   * Records concept statistics.
   *
   * @param concept the concept
   */
  public void recordConcept(final Concept concept) {
    if (statistics == null) {
      reset(null);
    }
    statistics.recordConcept(concept);
  }

  /**
   * Records hierarchy statistics.
   *
   * @param terminology the terminology
   * @param hierarchy the hierarchy
   * @throws Exception the exception
   */
  public void recordHierarchy(final Terminology terminology, final HierarchyUtils hierarchy)
      throws Exception {

    ensure(terminology);

    final TerminologyStats.HierarchyStats hierarchyStats = new TerminologyStats.HierarchyStats();
    statistics.setHierarchy(hierarchyStats);
    if (terminology == null
        || terminology.getMetadata() == null
        || terminology.getMetadata().getHierarchy() == null
        || !terminology.getMetadata().getHierarchy()
        || hierarchy == null) {
      hierarchyStats.setApplicable(false);
      return;
    }

    hierarchyStats.setApplicable(true);
    final HierarchyUtils.PathStats pathStats = hierarchy.getPathStats(terminology);
    hierarchyStats.setCodeCount(pathStats == null ? 0 : pathStats.getCodeCount());
    hierarchyStats.setRootCount(hierarchy.getHierarchyRoots().size());
    hierarchyStats.setTreePositionCount(pathStats == null ? 0 : pathStats.getTreePositionCount());
    hierarchyStats.setMinPaths(
        pathStats == null
            ? null
            : codeCount(pathStats.getMinPathsCode(), hierarchy, pathStats.getMinPathCount()));
    hierarchyStats.setMaxPaths(
        pathStats == null
            ? null
            : codeCount(pathStats.getMaxPathsCode(), hierarchy, pathStats.getMaxPathCount()));

    final String maxChildrenCode = hierarchy.getCodeWithMaxChildren(terminology);
    hierarchyStats.setMaxChildren(
        codeCount(maxChildrenCode, hierarchy, hierarchy.getChildCount(maxChildrenCode)));

    final String maxParentsCode = hierarchy.getCodeWithMaxParents(terminology);
    hierarchyStats.setMaxParents(
        codeCount(maxParentsCode, hierarchy, hierarchy.getParentCount(maxParentsCode)));
  }

  /**
   * Returns a code count object.
   *
   * @param code the code
   * @param hierarchy the hierarchy
   * @param count the count
   * @return the code count object
   */
  private TerminologyStats.CodeCount codeCount(
      final String code, final HierarchyUtils hierarchy, final long count) {
    return code == null
        ? null
        : new TerminologyStats.CodeCount(code, hierarchy.getName(code), count);
  }
}
