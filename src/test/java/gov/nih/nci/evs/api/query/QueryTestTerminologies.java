package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;

/**
 * Builds terminologies for query tests. Query nodes need a terminology's terminology and version,
 * and searches need its index name (see {@link EvsQuery}), so tests build them here, all at once,
 * instead of setting fields one at a time and missing one.
 */
public final class QueryTestTerminologies {

  /** Not instantiated: only static helpers. */
  private QueryTestTerminologies() {}

  /**
   * Builds a terminology version with its own concept index.
   *
   * @param terminologyName the terminology, e.g. "ncit"
   * @param version the version, e.g. "25.12e"
   * @param indexName the concept index name
   * @return the terminology
   */
  public static Terminology getTerminology(
      final String terminologyName, final String version, final String indexName) {
    final Terminology terminology = new Terminology();
    terminology.setTerminology(terminologyName);
    terminology.setVersion(version);
    terminology.setIndexName(indexName);
    return terminology;
  }
}
