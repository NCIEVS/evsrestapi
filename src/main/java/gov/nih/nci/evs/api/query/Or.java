package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.ArrayList;
import java.util.List;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches concepts that any child query matches. No children matches nothing, so an empty list of
 * alternatives, e.g. a role target that found no concepts, doesn't widen a search to everything.
 */
public class Or extends EvsQuery {

  /** The queries of which at least one must match. */
  private List<EvsQuery> children;

  /** Instantiates an empty {@link Or}. */
  public Or() {
    // n/a
  }

  /**
   * Instantiates an {@link Or}.
   *
   * @param children the queries of which at least one must match
   */
  public Or(final List<EvsQuery> children) {
    this.children = new ArrayList<>(children);
  }

  /**
   * Returns the queries of which at least one must match.
   *
   * @return the children
   */
  public List<EvsQuery> getChildren() {
    if (children == null) {
      children = new ArrayList<>();
    }
    return children;
  }

  /**
   * Sets the queries of which at least one must match.
   *
   * @param children the children
   */
  public void setChildren(final List<EvsQuery> children) {
    this.children = children;
  }

  /* see superclass */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    final boolean hasAlternatives = !children.isEmpty();
    if (!hasAlternatives) {
      // A bool query with no clauses would match everything, so match nothing explicitly
      return QueryBuilders.boolQuery().mustNot(QueryBuilders.matchAllQuery());
    }
    final int anyChildMatches = 1;
    final BoolQueryBuilder anyChildQuery =
        QueryBuilders.boolQuery().minimumShouldMatch(anyChildMatches);
    children.forEach(
        child -> anyChildQuery.should(child.fetchOpenSearchQuery(operations, terminologies)));
    return anyChildQuery;
  }
}
