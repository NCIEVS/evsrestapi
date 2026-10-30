package gov.nih.nci.evs.api.query;

import java.util.Objects;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches values exactly equal to the term (case sensitive).
 *
 * @param term the term the value must equal
 */
public record Exact(String term) implements SupportsIgnoringCase {

  /**
   * Instantiates an {@link Exact}.
   *
   * @param term the term the value must equal
   */
  public Exact {
    Objects.requireNonNull(term, "term");
  }

  /* see superclass */
  @Override
  public QueryBuilder getQueryBuilder(final String field) {
    return QueryBuilders.termQuery(field, term);
  }

  /* see superclass */
  @Override
  public QueryBuilder getCaseInsensitiveQueryBuilder(final String field) {
    return QueryBuilders.termQuery(field, term).caseInsensitive(true);
  }
}
