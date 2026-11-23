package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Terminology;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;

/**
 * Finds ancestors by reading the anchor concepts' stored paths to root. Every index stores {@code
 * paths}, whether or not it was indexed with ancestor fields, so this works on all of them. (The
 * {@code ancestors} field can't be read back: it's write-only, for matching only.)
 */
final class StoredPaths {

  /** Concepts fetched per lookup, so one lookup stays a reasonable size. */
  static final int CONCEPTS_PER_LOOKUP = 1_000;

  /** Field holding a concept's paths to root. */
  private static final String PATHS_FIELD = "paths";

  /** Field holding a concept's code. */
  private static final String CODE_FIELD = "code";

  /** Distance of the anchor itself from the anchor: the first step of each of its paths. */
  private static final int ANCHOR_DISTANCE = 0;

  /** Position of the anchor on each of its paths: the first step. */
  private static final int ANCHOR_POSITION = 0;

  /** The page every lookup asks for; each lookup is one page holding its whole batch. */
  private static final int FIRST_PAGE = 0;

  /** Position of the first code, where the first batch starts. */
  private static final int FIRST_CODE_POSITION = 0;

  /** Whether lookups fetch the stored document: yes, filtered to the code and paths. */
  private static final boolean SHOULD_FETCH_SOURCE = true;

  /** Not instantiated. */
  private StoredPaths() {
    // n/a
  }

  /**
   * Returns the codes above the anchors in one terminology, within {@code maxLevel} levels by
   * shortest distance, not including the anchors unless one is above another.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology, whose index holds the paths
   * @param anchorCodes the anchor codes in that terminology
   * @param maxLevel the maximum distance above the anchors, or null for any distance
   * @return the codes above the anchors
   */
  static Set<String> fetchAncestorCodes(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Collection<String> anchorCodes,
      final Integer maxLevel) {
    // Shortest distance from any anchor to each ancestor, across every path of every anchor
    final Map<String, Integer> distanceByAncestorCode = new HashMap<>();
    for (final Concept anchor : fetchConcepts(operations, terminology, anchorCodes)) {
      recordAncestorDistances(anchor, distanceByAncestorCode);
    }

    final Set<String> codes = getCodesWithinLevel(distanceByAncestorCode, maxLevel);
    checkSize(codes);

    return codes;
  }

  /**
   * Records the distance to each ancestor on every path of one anchor, keeping the shortest.
   *
   * @param anchor the anchor, with its paths
   * @param distanceByAncestorCode the shortest distances found so far, updated
   */
  private static void recordAncestorDistances(
      final Concept anchor, final Map<String, Integer> distanceByAncestorCode) {
    final boolean hasPaths = anchor.getPaths() != null && anchor.getPaths().getPaths() != null;
    if (!hasPaths) {
      return;
    }

    for (final Path path : anchor.getPaths().getPaths()) {
      recordPathDistances(anchor.getCode(), path, distanceByAncestorCode);
    }
  }

  /**
   * Records the distance to each ancestor on one path, keeping the shortest.
   *
   * @param anchorCode the anchor's code
   * @param path the path, starting at the anchor
   * @param distanceByAncestorCode the shortest distances found so far, updated
   */
  private static void recordPathDistances(
      final String anchorCode, final Path path, final Map<String, Integer> distanceByAncestorCode) {
    final List<ConceptMinimal> steps = path.getConcepts();
    for (int position = ANCHOR_POSITION; position < steps.size(); position++) {
      final ConceptMinimal step = steps.get(position);
      final int distance = getStepDistance(step, position);

      final boolean isAncestorStep =
          distance > ANCHOR_DISTANCE && !step.getCode().equals(anchorCode);
      if (isAncestorStep) {
        distanceByAncestorCode.merge(step.getCode(), distance, Math::min);
      }
    }
  }

  /**
   * Returns a path step's distance from the anchor: its stored level, or else its position.
   *
   * @param step the step
   * @param position its position on the path, the anchor being 0
   * @return the distance
   */
  private static int getStepDistance(final ConceptMinimal step, final int position) {
    final boolean hasLevel = step.getLevel() != null;
    return hasLevel ? step.getLevel() : position;
  }

  /**
   * Returns the ancestor codes within {@code maxLevel} of the anchors.
   *
   * @param distanceByAncestorCode the shortest distance to each ancestor
   * @param maxLevel the maximum distance, or null for any distance
   * @return the codes
   */
  private static Set<String> getCodesWithinLevel(
      final Map<String, Integer> distanceByAncestorCode, final Integer maxLevel) {
    return distanceByAncestorCode.entrySet().stream()
        .filter(entry -> getIsWithinLevel(entry.getValue(), maxLevel))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  /**
   * Indicates whether a distance is within {@code maxLevel}.
   *
   * @param distance the distance
   * @param maxLevel the maximum distance, or null for any distance
   * @return true if within it
   */
  private static boolean getIsWithinLevel(final int distance, final Integer maxLevel) {
    final boolean hasLevelLimit = maxLevel != null;
    if (!hasLevelLimit) {
      return true;
    }

    return distance <= maxLevel;
  }

  /**
   * Rejects ancestor sets too large to match in one {@code terms} query.
   *
   * @param codes the ancestor codes
   */
  private static void checkSize(final Set<String> codes) {
    final boolean isTooLarge = codes.size() > MatchAnyOf.MAX_VALUES;
    if (isTooLarge) {
      throw new IllegalArgumentException(
          "Too many ancestors to match in one query: "
              + codes.size()
              + " > "
              + MatchAnyOf.MAX_VALUES);
    }
  }

  /**
   * Fetches concepts by code from one terminology's index, with only the code and paths, a batch at
   * a time.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology
   * @param codes the codes
   * @return the concepts found
   */
  private static List<Concept> fetchConcepts(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Collection<String> codes) {
    final List<Concept> concepts = new ArrayList<>();
    for (final List<String> batch : getBatches(codes)) {
      concepts.addAll(fetchBatch(operations, terminology, batch));
    }

    return concepts;
  }

  /**
   * Fetches one batch of concepts by code, with only the code and paths.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology
   * @param batch the codes, at most {@link #CONCEPTS_PER_LOOKUP}
   * @return the concepts found
   */
  private static List<Concept> fetchBatch(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final List<String> batch) {
    final NativeSearchQuery lookup =
        new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.idsQuery().addIds(batch.toArray(String[]::new)))
            .withSourceFilter(
                new FetchSourceFilter(
                    SHOULD_FETCH_SOURCE, new String[] {CODE_FIELD, PATHS_FIELD}, null))
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
}
