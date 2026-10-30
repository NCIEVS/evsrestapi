package gov.nih.nci.evs.api.query;

import java.util.Objects;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches values that begin with the term (case sensitive).
 *
 * @param term the prefix the value must begin with
 */
public record StartsWith(String term) implements SupportsIgnoringCase {

  /**
   * Instantiates a {@link StartsWith}.
   *
   * @param term the prefix the value must begin with
   */
  public StartsWith {
    Objects.requireNonNull(term, "term");
  }

  /* see superclass */
  @Override
  public QueryBuilder getQueryBuilder(final String field) {
    return QueryBuilders.prefixQuery(field, term);
  }

  /* see superclass */
  @Override
  public QueryBuilder getCaseInsensitiveQueryBuilder(final String field) {
    return QueryBuilders.prefixQuery(field, term).caseInsensitive(true);
  }
}
