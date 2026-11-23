package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.ArrayList;
import java.util.List;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/** Matches concepts that every child query matches. No children matches everything. */
public class And extends EvsQuery {

  /** The queries that must all match. */
  private List<EvsQuery> children;

  /** Instantiates an empty {@link And}. */
  public And() {
    // n/a
  }

  /**
   * Instantiates an {@link And}.
   *
   * @param children the queries that must all match
   */
  public And(final List<EvsQuery> children) {
    this.children = new ArrayList<>(children);
  }

  /**
   * Returns the queries that must all match.
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
   * Sets the queries that must all match.
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
    final BoolQueryBuilder allChildrenQuery = QueryBuilders.boolQuery();
    children.forEach(
        child -> allChildrenQuery.must(child.fetchOpenSearchQuery(operations, terminologies)));
    return allChildrenQuery;
  }
}
