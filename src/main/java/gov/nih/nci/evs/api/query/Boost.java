package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import java.util.Objects;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches what the child query matches, but scores every match the same fixed amount, ignoring how
 * well it matched. Inside an {@link Or}, the scores of the alternatives that matched add up, so
 * boosted alternatives sort matches into tiers.
 */
public class Boost extends EvsQuery {

  /** The query whose matches are scored. */
  private EvsQuery child;

  /** The score every match gets. */
  private float score;

  /** Instantiates an empty {@link Boost}. */
  public Boost() {
    // n/a
  }

  /**
   * Instantiates a {@link Boost}.
   *
   * @param child the query whose matches are scored
   * @param score the score every match gets
   */
  public Boost(final EvsQuery child, final float score) {
    this.child = Objects.requireNonNull(child, "child");
    this.score = score;
  }

  /**
   * Returns the query whose matches are scored.
   *
   * @return the child
   */
  public EvsQuery getChild() {
    return child;
  }

  /**
   * Sets the query whose matches are scored.
   *
   * @param child the child
   */
  public void setChild(final EvsQuery child) {
    this.child = child;
  }

  /**
   * Returns the score every match gets.
   *
   * @return the score
   */
  public float getScore() {
    return score;
  }

  /**
   * Sets the score every match gets.
   *
   * @param score the score
   */
  public void setScore(final float score) {
    this.score = score;
  }

  /* see superclass */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    return QueryBuilders.constantScoreQuery(child.fetchOpenSearchQuery(operations, terminologies))
        .boost(score);
  }
}
