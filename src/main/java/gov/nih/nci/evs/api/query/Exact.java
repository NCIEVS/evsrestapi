package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.service.OpenSearchServiceImpl;
import org.opensearch.index.query.QueryBuilder;

/**
 * Finds concepts whose name, synonym, or code is the term, like the legacy search type {@code
 * match}.
 */
public class Exact extends TermSearch {

  /** Instantiates an empty {@link Exact}. */
  public Exact() {
    // n/a
  }

  /**
   * Instantiates an {@link Exact}.
   *
   * @param term the term to search for
   */
  public Exact(final String term) {
    super(term);
  }

  /* see superclass */
  @Override
  protected QueryBuilder getTermQuery(final String escapedTerm) throws Exception {
    final boolean isPrefix = false;
    return OpenSearchServiceImpl.getMatchQuery(escapedTerm, isPrefix);
  }
}
