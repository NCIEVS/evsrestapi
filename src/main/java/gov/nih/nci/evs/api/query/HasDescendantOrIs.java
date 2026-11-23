package gov.nih.nci.evs.api.query;

import static gov.nih.nci.evs.api.query.RawBoolQueries.getAnyOfQuery;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;

/**
 * Matches the target concepts and every concept above them, in the same terminology version: the
 * upward counterpart of {@link HasAncestorOrIs}. For example, Breast Carcinoma and everything above
 * it. {@code maxLevel} limits how far above the targets to go (the targets themselves are distance
 * 0 and always match).
 */
public class HasDescendantOrIs extends HasDescendant {

  /** Instantiates an empty {@link HasDescendantOrIs}. */
  public HasDescendantOrIs() {
    // n/a
  }

  /**
   * Instantiates an {@link HasDescendantOrIs} for the target concepts and everything above them.
   *
   * @param target the target concept's name or code
   */
  public HasDescendantOrIs(final String target) {
    super(target);
  }

  /**
   * Instantiates an {@link HasDescendantOrIs} for the target concepts and those within {@code
   * maxLevel} levels above them.
   *
   * @param target the target concept's name or code
   * @param maxLevel the maximum distance above the targets, at least 1
   */
  public HasDescendantOrIs(final String target, final int maxLevel) {
    super(target, maxLevel);
  }

  /**
   * Returns the query matching the targets' ancestors, or the targets themselves.
   *
   * @param operations the OpenSearch operations
   * @param terminologies the terminologies being searched
   * @return the query builder
   */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    return getAnyOfQuery(
        List.of(
            super.fetchOpenSearchQuery(operations, terminologies),
            getTargetsQuery(operations, terminologies)));
  }
}
