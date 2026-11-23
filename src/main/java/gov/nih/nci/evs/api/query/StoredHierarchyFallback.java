package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;

/**
 * Finds descendants for terminologies indexed before concepts carried {@code ancestors} and {@code
 * ancestorDepths} (their metadata lacks {@code hierarchyFields}). Those indexes can't match "below
 * X" in one query, so this reads the hierarchy stored on the anchor concepts and the hierarchy
 * nodes match the codes it returns.
 *
 * <p>TODO: Remove this class once every terminology is reindexed with ancestor fields. The indexed
 * path in {@link HasAncestor} then covers everything.
 */
final class StoredHierarchyFallback {

  /** Concepts fetched per lookup, so one lookup stays a reasonable size. */
  static final int CONCEPTS_PER_LOOKUP = 1_000;

  /** Field holding a concept's precomputed descendants (a depth-first list). */
  private static final String DESCENDANTS_FIELD = "descendants";

  /** Field holding a concept's direct children. */
  private static final String CHILDREN_FIELD = "children";

  /** Field holding a concept's code. */
  private static final String CODE_FIELD = "code";

  /** The first level below the anchors: their direct children. */
  private static final int FIRST_LEVEL = 1;

  /** The page every lookup asks for; each lookup is one page holding its whole batch. */
  private static final int FIRST_PAGE = 0;

  /** Position of the first code, where the first batch starts. */
  private static final int FIRST_CODE_POSITION = 0;

  /** Whether lookups fetch the stored document: yes, filtered to the code and one field. */
  private static final boolean SHOULD_FETCH_SOURCE = true;

  /** Not instantiated. */
  private StoredHierarchyFallback() {
    // n/a
  }

  /**
   * Returns the codes below the anchors in one terminology, not including the anchors unless one is
   * below another.
   *
   * <p>Without a depth limit, this reads each anchor's precomputed descendant list. With one, it
   * walks children breadth-first from all anchors at once: the precomputed list records the depth
   * at which a depth-first walk first reached a concept, which under polyhierarchy can be larger
   * than the shortest distance, so filtering it by depth could miss concepts.
   *
   * <p>TODO: Remove with this class once every terminology is reindexed with ancestor fields.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology, whose index holds the stored hierarchy
   * @param anchorCodes the anchor codes in that terminology
   * @param maxLevel the maximum depth below the anchors, or null for any depth
   * @return the codes below the anchors
   */
  static Set<String> fetchDescendantCodes(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Collection<String> anchorCodes,
      final Integer maxLevel) {
    final boolean hasLevelLimit = maxLevel != null;
    if (!hasLevelLimit) {
      return fetchAllDescendantCodes(operations, terminology, anchorCodes);
    }

    return fetchDescendantCodesWithinLevel(operations, terminology, anchorCodes, maxLevel);
  }

  /**
   * Returns every code in the anchors' precomputed descendant lists.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology
   * @param anchorCodes the anchor codes
   * @return the descendant codes
   */
  private static Set<String> fetchAllDescendantCodes(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Collection<String> anchorCodes) {
    final Set<String> codes = new HashSet<>();
    for (final Concept anchor :
        fetchConcepts(operations, terminology, anchorCodes, DESCENDANTS_FIELD)) {
      codes.addAll(getCodes(anchor.getDescendants()));
      checkSize(codes);
    }

    return codes;
  }

  /**
   * Returns the codes within {@code maxLevel} levels below the anchors, by shortest distance,
   * walking children one level at a time from all anchors at once. A level with no concepts makes
   * no lookups, so the walk costs nothing once it runs out of children.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology
   * @param anchorCodes the anchor codes
   * @param maxLevel the maximum depth below the anchors
   * @return the descendant codes
   */
  private static Set<String> fetchDescendantCodesWithinLevel(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Collection<String> anchorCodes,
      final int maxLevel) {
    final Set<String> codes = new HashSet<>();

    Set<String> levelCodes = new HashSet<>(anchorCodes);
    for (int level = FIRST_LEVEL; level <= maxLevel; level++) {
      levelCodes = fetchUnseenChildCodes(operations, terminology, levelCodes, codes);
      checkSize(codes);
    }

    return codes;
  }

  /**
   * Returns the children of the parents that haven't been seen yet, adding them to the seen codes.
   * The first level a code is reached at is its shortest distance, so codes already seen are
   * skipped.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology
   * @param parentCodes the codes on the current level
   * @param seenCodes the codes found so far, added to
   * @return the codes on the next level
   */
  private static Set<String> fetchUnseenChildCodes(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Set<String> parentCodes,
      final Set<String> seenCodes) {
    final Set<String> childCodes = new HashSet<>();
    for (final Concept parent :
        fetchConcepts(operations, terminology, parentCodes, CHILDREN_FIELD)) {
      childCodes.addAll(recordUnseenCodes(parent.getChildren(), seenCodes));
    }

    return childCodes;
  }

  /**
   * Adds the concepts' codes to the seen codes and returns the ones that weren't there yet.
   *
   * @param concepts the concepts
   * @param seenCodes the codes found so far, added to
   * @return the codes seen for the first time
   */
  private static Set<String> recordUnseenCodes(
      final List<Concept> concepts, final Set<String> seenCodes) {
    final Set<String> unseenCodes = new HashSet<>();
    for (final Concept concept : concepts) {
      final boolean isFirstSeen = seenCodes.add(concept.getCode());
      if (isFirstSeen) {
        unseenCodes.add(concept.getCode());
      }
    }

    return unseenCodes;
  }

  /**
   * Returns the concepts' codes.
   *
   * @param concepts the concepts
   * @return the codes
   */
  private static List<String> getCodes(final List<Concept> concepts) {
    return concepts.stream().map(Concept::getCode).toList();
  }

  /**
   * Fetches concepts by code from one terminology's index, with only the code and one hierarchy
   * field, a batch at a time.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology
   * @param codes the codes
   * @param hierarchyField the hierarchy field to fetch
   * @return the concepts found
   */
  private static List<Concept> fetchConcepts(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Collection<String> codes,
      final String hierarchyField) {
    final List<Concept> concepts = new ArrayList<>();
    for (final List<String> batch : getBatches(codes)) {
      concepts.addAll(fetchBatch(operations, terminology, batch, hierarchyField));
    }

    return concepts;
  }

  /**
   * Fetches one batch of concepts by code, with only the code and one hierarchy field.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology
   * @param batch the codes, at most {@link #CONCEPTS_PER_LOOKUP}
   * @param hierarchyField the hierarchy field to fetch
   * @return the concepts found
   */
  private static List<Concept> fetchBatch(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final List<String> batch,
      final String hierarchyField) {
    final NativeSearchQuery lookup =
        new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.idsQuery().addIds(batch.toArray(String[]::new)))
            .withSourceFilter(
                new FetchSourceFilter(
                    SHOULD_FETCH_SOURCE, new String[] {CODE_FIELD, hierarchyField}, null))
            .withPageable(PageRequest.of(FIRST_PAGE, batch.size()))
            .build();

    return operations
        .search(lookup, Concept.class, IndexCoordinates.of(terminology.getIndexName()))
        .getSearchHits()
        .stream()
        .map(SearchHit::getContent)
        .toList();
  }

  /**
   * Splits the codes into batches of at most {@link #CONCEPTS_PER_LOOKUP}.
   *
   * @param codes the codes
   * @return the batches
   */
  private static List<List<String>> getBatches(final Collection<String> codes) {
    final List<String> remaining = new ArrayList<>(codes);

    final List<List<String>> batches = new ArrayList<>();
    for (int start = FIRST_CODE_POSITION; start < remaining.size(); start += CONCEPTS_PER_LOOKUP) {
      final int end = Math.min(start + CONCEPTS_PER_LOOKUP, remaining.size());
      batches.add(remaining.subList(start, end));
    }

    return batches;
  }

  /**
   * Rejects expansions too large to match in one {@code terms} query.
   *
   * @param codes the codes found so far
   */
  private static void checkSize(final Set<String> codes) {
    final boolean isTooLarge = codes.size() > MatchAnyOf.MAX_VALUES;
    if (isTooLarge) {
      throw new IllegalArgumentException(
          "Branch is too large for an index without ancestor fields (more than "
              + MatchAnyOf.MAX_VALUES
              + " concepts); reindex the terminology to support it");
    }
  }
}
