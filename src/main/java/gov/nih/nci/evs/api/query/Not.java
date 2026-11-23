package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import java.util.Objects;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches concepts that the child query doesn't match, including concepts missing the child's
 * field. E.g. {@code new Not(new InSubset(send))} keeps concepts outside SEND, including concepts
 * in no subset at all.
 *
 * <p>Adds nothing to the score, so wrapping it in an {@link And} doesn't change how the other
 * children rank.
 */
public class Not extends EvsQuery {

  /** The query concepts must not match. */
  private EvsQuery child;

  /** Instantiates an empty {@link Not}. */
  public Not() {
    // n/a
  }

  /**
   * Instantiates a {@link Not}.
   *
   * @param child the query concepts must not match
   */
  public Not(final EvsQuery child) {
    this.child = Objects.requireNonNull(child, "child");
  }

  /**
   * Returns the query concepts must not match.
   *
   * @return the child
   */
  public EvsQuery getChild() {
    return child;
  }

  /**
   * Sets the query concepts must not match.
   *
   * @param child the child
   */
  public void setChild(final EvsQuery child) {
    this.child = child;
  }

  /* see superclass */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    // A bool query with only must_not would score every match 1; a match_all filter scores 0
    return QueryBuilders.boolQuery()
        .filter(QueryBuilders.matchAllQuery())
        .mustNot(child.fetchOpenSearchQuery(operations, terminologies));
  }
}
