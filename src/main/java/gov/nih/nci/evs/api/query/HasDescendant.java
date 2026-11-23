package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import java.util.Set;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;

/**
 * Matches concepts strictly above any target concept, in the same terminology version: the upward
 * counterpart of {@link HasAncestor}. A target is a concept whose name or code is the given one.
 * For example, everything above Breast Carcinoma.
 *
 * <p>Without a level limit, this is one query: each concept stores its descendants' names and codes
 * in {@code descendants}, so a concept is an ancestor of a target when one of its descendants
 * matches.
 *
 * <p>An optional {@code maxLevel} limits how far above: 1 means direct parents. Distance is the
 * shortest, so a concept that is a direct parent of X on one path and further up on another counts
 * as 1. The {@code descendants} entries can't answer that yet (their levels aren't tied to their
 * codes, and aren't the shortest distance), so the target codes are looked up first (see {@link
 * HasHierarchyRelative#fetchTargetCodes}) and the ancestors come from the targets' stored paths to
 * root.
 *
 * @see HasDescendantOrIs
 */
public class HasDescendant extends HasHierarchyRelative {

  /** Field holding a descendant's name, on each of the concept's descendants. Analyzed text. */
  private static final String DESCENDANT_NAME_FIELD = "descendants.name";

  /** Field holding a descendant's code, on each of the concept's descendants. */
  private static final String DESCENDANT_CODE_FIELD = "descendants.code";

  /** Instantiates an empty {@link HasDescendant}. */
  public HasDescendant() {
    // n/a
  }

  /**
   * Instantiates an {@link HasDescendant} for everything above the target concepts.
   *
   * @param target the target concept's name or code
   */
  public HasDescendant(final String target) {
    super(target, ANY_LEVEL);
  }

  /**
   * Instantiates an {@link HasDescendant} for concepts within {@code maxLevel} levels above the
   * target concepts.
   *
   * @param target the target concept's name or code
   * @param maxLevel the maximum distance above the targets, at least 1
   */
  public HasDescendant(final String target, final int maxLevel) {
    super(target, maxLevel);
  }

  /* see superclass */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    final boolean hasLevelLimit = getMaxLevel() != null;
    if (hasLevelLimit) {
      return fetchRelativesOfTargetCodesQuery(operations, terminologies);
    }

    return new NameOrCodeMatch(DESCENDANT_NAME_FIELD, DESCENDANT_CODE_FIELD, Set.of(getTarget()))
        .fetchOpenSearchQuery(operations, terminologies);
  }

  /* see superclass */
  @Override
  protected QueryBuilder fetchRelativesQuery(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Set<String> targetCodes) {
    final Set<String> ancestorCodes =
        StoredPaths.fetchAncestorCodes(operations, terminology, targetCodes, getMaxLevel());

    return getCodeInQuery(operations, terminology, ancestorCodes);
  }
}
