package gov.nih.nci.evs.api.query;

import java.util.Objects;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches values that include the term anywhere (case sensitive).
 *
 * @param term the substring the value must include
 */
public record Contains(String term) implements SupportsIgnoringCase {

  /** Wildcard that matches any sequence of characters. */
  private static final String ANY_CHARACTERS = "*";

  /**
   * Instantiates a {@link Contains}.
   *
   * @param term the substring the value must include
   */
  public Contains {
    Objects.requireNonNull(term, "term");
  }

  /* see superclass */
  @Override
  public QueryBuilder getQueryBuilder(final String field) {
    return QueryBuilders.wildcardQuery(field, getPattern());
  }

  /* see superclass */
  @Override
  public QueryBuilder getCaseInsensitiveQueryBuilder(final String field) {
    return QueryBuilders.wildcardQuery(field, getPattern()).caseInsensitive(true);
  }

  /**
   * Returns the wildcard pattern that matches the term anywhere in a value.
   *
   * @return the pattern
   */
  private String getPattern() {
    return ANY_CHARACTERS + escapeWildcards(term) + ANY_CHARACTERS;
  }

  /**
   * Escapes wildcard syntax so the term is matched literally.
   *
   * @param value the raw term
   * @return the escaped term
   */
  static String escapeWildcards(final String value) {
    // Escape the escape character first so later escapes are not doubled
    return value.replace("\\", "\\\\").replace("*", "\\*").replace("?", "\\?");
  }
}
