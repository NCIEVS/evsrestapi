package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.service.OpenSearchServiceImpl;
import org.opensearch.index.query.QueryBuilder;

/**
 * Finds concepts whose name, synonym, or code starts with the term, like the legacy search type
 * {@code startsWith}.
 */
public class StartsWith extends TermSearch {

  /** Instantiates an empty {@link StartsWith}. */
  public StartsWith() {
    // n/a
  }

  /**
   * Instantiates a {@link StartsWith}.
   *
   * @param term the term to search for
   */
  public StartsWith(final String term) {
    super(term);
  }

  /* see superclass */
  @Override
  protected QueryBuilder getTermQuery(final String escapedTerm) throws Exception {
    final boolean isPrefix = true;
    return OpenSearchServiceImpl.getMatchQuery(escapedTerm, isPrefix);
  }
}
