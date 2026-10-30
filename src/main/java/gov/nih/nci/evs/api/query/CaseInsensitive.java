package gov.nih.nci.evs.api.query;

import java.util.Objects;
import org.opensearch.index.query.QueryBuilder;

/**
 * Runs the wrapped match ignoring case, so "MELANOMA" matches both "Melanoma" and "melanoma". The
 * wrapped match is not changed and remains case sensitive on its own.
 *
 * <p>Only matches that implement {@link SupportsIgnoringCase} can be wrapped. {@link Fuzzy} cannot,
 * because OpenSearch has no case-insensitive fuzzy query.
 *
 * @param match the match to run ignoring case
 */
public record CaseInsensitive(SupportsIgnoringCase match) implements ValueMatch {

  /**
   * Instantiates a {@link CaseInsensitive}.
   *
   * @param match the match to run ignoring case
   */
  public CaseInsensitive {
    Objects.requireNonNull(match, "match");
  }

  /* see superclass */
  @Override
  public QueryBuilder getQueryBuilder(final String field) {
    return match.getCaseInsensitiveQueryBuilder(field);
  }
}
