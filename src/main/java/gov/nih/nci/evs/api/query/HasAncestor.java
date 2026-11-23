package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;

/**
 * Matches concepts strictly below any target concept, in the same terminology version. A target is
 * a concept whose name or code is the given one. For example, everything below Melanoma ({@code new
 * HasAncestor("C3224")}).
 *
 * <p>An optional {@code maxLevel} limits how far below: 1 means direct children. Depth is the
 * shortest distance, so a concept that is a direct child of X on one path and deeper on another
 * counts as depth 1.
 *
 * <p>The target codes are looked up first (see {@link HierarchyQuery#fetchTargetCodes}).
 * Terminologies indexed with ancestor fields then match with one query on {@code ancestors.code}
 * (or {@code ancestorDepths} for a depth limit). Older indexes fall back to reading the stored
 * hierarchy (see {@link StoredHierarchyFallback}).
 * <p>The {@code ancestors} field only holds codes, so the target codes are looked up first (see
 * {@link HasHierarchyRelative#fetchTargetCodes}). Terminologies indexed with ancestor fields then
 * match with one query on {@code ancestors} (or {@code ancestorDepths} for a depth limit). Older
 * indexes fall back to reading the stored hierarchy (see {@link StoredHierarchyFallback}).
 *
 * @see HasAncestorOrIs
 * @see HasDescendant
 */
public class HasAncestor extends HasHierarchyRelative {

  /** Field holding the code of each of a concept's ancestors. */
  static final String ANCESTORS_FIELD = "ancestors.code";

  /** Field listing "code|depth" tokens, each ancestor with its shortest distance. */
  private static final String ANCESTOR_DEPTHS_FIELD = "ancestorDepths";

  /** Separator between code and depth in {@link #ANCESTOR_DEPTHS_FIELD}. */
  static final String DEPTH_SEPARATOR = "|";

  /** The first depth below a target: its direct children. */
  private static final int FIRST_DEPTH = 1;

  /** Instantiates an empty {@link HasAncestor}. */
  public HasAncestor() {
    // n/a
  }

  /**
   * Instantiates a {@link HasAncestor} for everything below the target concepts.
   *
   * @param target the target concept's name or code
   */
  public HasAncestor(final String target) {
    super(target, ANY_LEVEL);
  }

  /**
   * Instantiates a {@link HasAncestor} for concepts within {@code maxLevel} levels below the target
   * concepts.
   *
   * @param target the target concept's name or code
   * @param maxLevel the maximum depth below the targets, at least 1
   */
  public HasAncestor(final String target, final int maxLevel) {
    super(target, maxLevel);
  }

  /* see superclass */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    return fetchRelativesOfTargetCodesQuery(operations, terminologies);
  }

  /* see superclass */
  @Override
  protected QueryBuilder fetchRelativesQuery(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Set<String> targetCodes) {
    final boolean hasAncestorFields = getHasAncestorFields(terminology);
    if (hasAncestorFields) {
      return getBelowTargetsQuery(operations, terminology, targetCodes);
    }

    // TODO: Remove the fallback once every terminology is reindexed with ancestor fields
    return fetchBelowTargetsFallbackQuery(operations, terminology, targetCodes);
  }

  /**
   * Returns "code|depth" tokens for every target and every depth from 1 to {@code maxLevel}, which
   * match {@link #ANCESTOR_DEPTHS_FIELD}. For example C3262 with depth 2 gives C3262|1 and C3262|2.
   *
   * @param targetCodes the target codes
   * @param maxLevel the maximum depth
   * @return the tokens
   */
  static List<String> getDepthTokens(final Collection<String> targetCodes, final int maxLevel) {
    final long tokenCount = (long) targetCodes.size() * maxLevel;
    final boolean hasTooManyTokens = tokenCount > MatchAnyOf.MAX_VALUES;
    if (hasTooManyTokens) {
      throw new IllegalArgumentException(
          "Too many targets for maxLevel "
              + maxLevel
              + ": "
              + targetCodes.size()
              + " targets gives more than "
              + MatchAnyOf.MAX_VALUES
              + " depth tokens");
    }

    final List<String> tokens = new ArrayList<>();
    for (final String code : targetCodes) {
      tokens.addAll(getTargetDepthTokens(code, maxLevel));
    }

    return tokens;
  }

  /**
   * Returns the "code|depth" tokens for one target and every depth from 1 to {@code maxLevel}.
   *
   * @param code the target code
   * @param maxLevel the maximum depth
   * @return the tokens
   */
  private static List<String> getTargetDepthTokens(final String code, final int maxLevel) {
    final List<String> tokens = new ArrayList<>();
    for (int depth = FIRST_DEPTH; depth <= maxLevel; depth++) {
      tokens.add(code + DEPTH_SEPARATOR + depth);
    }

    return tokens;
  }

  /**
   * Returns the condition for concepts below the targets, using the indexed ancestor fields.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology the targets are from
   * @param targetCodes the target codes
   * @return the condition
   */
  private QueryBuilder getBelowTargetsQuery(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Set<String> targetCodes) {
    final boolean hasLevelLimit = getMaxLevel() != null;
    if (!hasLevelLimit) {
      return new MatchAnyOf(ANCESTORS_FIELD, targetCodes)
          .fetchOpenSearchQuery(operations, List.of(terminology));
    }

    return new MatchAnyOf(ANCESTOR_DEPTHS_FIELD, getDepthTokens(targetCodes, getMaxLevel()))
        .fetchOpenSearchQuery(operations, List.of(terminology));
  }

  /**
   * Returns the condition for concepts below the targets in an index without ancestor fields, by
   * expanding the targets from the stored hierarchy.
   *
   * <p>TODO: Remove once every terminology is reindexed with ancestor fields.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology
   * @param targetCodes the target codes
   * @return the condition
   */
  private QueryBuilder fetchBelowTargetsFallbackQuery(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Set<String> targetCodes) {
    final Set<String> descendantCodes =
        StoredHierarchyFallback.fetchDescendantCodes(
            operations, terminology, targetCodes, getMaxLevel());

    return getCodeInQuery(operations, terminology, descendantCodes);
  }

  /**
   * Indicates whether a terminology's index has the {@code ancestors} and {@code ancestorDepths}
   * fields.
   *
   * <p>TODO: Remove with the fallback; every index will have them.
   *
   * @param terminology the terminology
   * @return true if the fields are indexed
   */
  private static boolean getHasAncestorFields(final Terminology terminology) {
    final TerminologyMetadata metadata = terminology.getMetadata();
    final boolean hasMetadata = metadata != null;
    if (!hasMetadata) {
      return false;
    }

    return Boolean.TRUE.equals(metadata.getHierarchyFields());
  }
}
