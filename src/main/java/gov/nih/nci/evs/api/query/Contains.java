package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.service.OpenSearchServiceImpl;
import org.opensearch.index.query.QueryBuilder;

/**
 * Finds concepts whose name, synonym, definition, or code contains the words of the term, like the
 * legacy search type {@code contains}.
 */
public class Contains extends TermSearch {

  /** The legacy search type this node mirrors. */
  private static final String SEARCH_TYPE = "contains";

  /** Instantiates an empty {@link Contains}. */
  public Contains() {
    // n/a
  }

  /**
   * Instantiates a {@link Contains}.
   *
   * @param term the term to search for
   */
  public Contains(final String term) {
    super(term);
  }

  /* see superclass */
  @Override
  protected QueryBuilder getTermQuery(final String escapedTerm) throws Exception {
    final SearchCriteria criteria = new SearchCriteria();
    criteria.setType(SEARCH_TYPE);
    final boolean isFuzzy = false;
    final boolean isAllWords = false;
    return OpenSearchServiceImpl.getContainsQuery(criteria, escapedTerm, isFuzzy, isAllWords);
  }
}
