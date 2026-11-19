package gov.nih.nci.evs.api.support.es;

/**
 * The OpenSearch server limits the searches depend on. A limit of OpenSearch limits every search
 * built on it, so the code that builds a search checks these up front and reports a breach as a bad
 * request, instead of letting OpenSearch fail the search. {@code OpenSearchLimitsTest} checks that
 * the running server matches.
 */
public final class OpenSearchLimits {

  /**
   * The most values one {@code terms} query accepts: {@code index.max_terms_count}, left at
   * OpenSearch's default.
   */
  public static final int MAX_TERMS_COUNT = 65_536;

  /**
   * The furthest a search can page, {@code from + size}: {@code index.max_result_window}, which
   * {@code reindex.sh} raises from OpenSearch's default of 10,000 on every concept index.
   */
  public static final int MAX_RESULT_WINDOW = 300_000;

  /**
   * The most clauses one query can hold, counting the clauses of every {@code bool} inside it:
   * {@code indices.query.bool.max_clause_count}, left at OpenSearch's default.
   */
  public static final int MAX_CLAUSE_COUNT = 1_024;

  /** Instantiates nothing: constants only. */
  private OpenSearchLimits() {
    // n/a
  }
}
