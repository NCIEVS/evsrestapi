package gov.nih.nci.evs.api.query;

import java.util.List;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MatchNoneQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Raw OpenSearch combining helpers for nodes that combine conditions before the logical operator
 * nodes land (see the package README, "Logical operators: use raw OpenSearch for now"). Each helper
 * maps onto one logical node, so the later refactor replaces calls one for one.
 *
 * <p>TODO: Replace with the {@code And}, {@code Or}, and {@code Not} nodes once they're tested.
 */
final class RawBoolQueries {

  /** How many {@code should} clauses must match for {@link #getAnyOfQuery}: at least one. */
  private static final int MIN_MATCHING_CLAUSES = 1;

  /** Not instantiated. */
  private RawBoolQueries() {
    // n/a
  }

  /**
   * Returns a query matching only documents that match all the conditions. To become the {@code
   * And} node.
   *
   * @param conditions the conditions
   * @return the combined query
   */
  static QueryBuilder getAllOfQuery(final QueryBuilder... conditions) {
    final BoolQueryBuilder bool = QueryBuilders.boolQuery();
    for (final QueryBuilder condition : conditions) {
      bool.must(condition);
    }

    return bool;
  }

  /**
   * Returns a query matching documents that match at least one of the conditions. To become the
   * {@code Or} node.
   *
   * @param conditions the conditions
   * @return the combined query
   */
  static QueryBuilder getAnyOfQuery(final List<QueryBuilder> conditions) {
    final BoolQueryBuilder bool =
        QueryBuilders.boolQuery().minimumShouldMatch(MIN_MATCHING_CLAUSES);
    for (final QueryBuilder condition : conditions) {
      bool.should(condition);
    }

    return bool;
  }

  /**
   * Returns a query no document matches. To become a {@code Not} of match-all, or a constant node.
   *
   * @return the query
   */
  static QueryBuilder getMatchNothingQuery() {
    return new MatchNoneQueryBuilder();
  }
}
