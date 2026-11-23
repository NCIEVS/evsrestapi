package gov.nih.nci.evs.api.query;

import static gov.nih.nci.evs.api.query.RawBoolQueries.getAllOfQuery;
import static gov.nih.nci.evs.api.query.RawBoolQueries.getAnyOfQuery;
import static gov.nih.nci.evs.api.query.RawBoolQueries.getMatchNothingQuery;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * Base type for nodes that match concepts with a hierarchy relative among the target concepts:
 * {@link HasAncestor} (concepts below a target), {@link HasDescendant} (concepts above a target).
 * The targets are the concepts whose name or code is {@code target}, so these don't nest other
 * queries.
 *
 * <p>Some cases can't be answered from the concept's own fields yet, and look up the target codes
 * first ({@link #fetchTargetCodes}). Codes are only unique within one terminology version, and each
 * version has its own index, so each terminology's targets only match relatives in that
 * terminology's index. Subclasses decide how to find the relatives in {@link #fetchRelativesQuery}.
 *
 * <p>TODO: Remove the lookup once ancestors and descendants are indexed as nested {code, name,
 * level} entries.
 *
 * <p>An optional {@code maxLevel} limits how many levels away relatives may be, by shortest
 * distance: 1 means direct children (or parents).
 */
public abstract class HasHierarchyRelative extends EvsQuery {

  /** Field holding a document's identifier, which is the concept code. */
  private static final String ID_FIELD = "_id";

  /** Metadata field naming the index a document is in; one index per terminology version. */
  private static final String INDEX_FIELD = "_index";

  /** Field holding the concept's name. Analyzed text. */
  private static final String NAME_FIELD = "name";

  /** Field holding the concept's code. */
  private static final String CODE_FIELD = "code";

  /** The smallest allowed {@code maxLevel}: direct children or parents. */
  private static final int MIN_MAX_LEVEL = 1;

  /** A {@code maxLevel} of any distance, for constructors without a limit. */
  static final Integer ANY_LEVEL = null;

  /** Concepts fetched per page by {@link #fetchTargetCodes}, at OpenSearch's default page limit. */
  static final int CONCEPTS_PER_PAGE = 10_000;

  /**
   * Stored fields value that fetches none, not even {@code _source} or {@code _id}, so hits are
   * identified from their sort values without reading any stored document. For efficiency: see
   * {@link #fetchTargetCodes}.
   */
  static final String NO_STORED_FIELDS = "_none_";

  /** Where a hit's code is among its sort values: target pages sort by code. */
  private static final int CODE_SORT_VALUE_POSITION = 0;

  /**
   * The page number every target page asks for. The page cursor, not the page number, moves through
   * the pages.
   */
  private static final int FIRST_PAGE = 0;

  /** The target concepts' name or code. */
  private String target;

  /** The maximum distance from the targets, or null for any distance. */
  private Integer maxLevel;

  /** Instantiates an empty {@link HasHierarchyRelative}. */
  protected HasHierarchyRelative() {
    // n/a
  }

  /**
   * Instantiates a {@link HasHierarchyRelative}.
   *
   * @param target the target concepts' name or code
   * @param maxLevel the maximum distance from the targets, at least 1, or null for any distance
   */
  protected HasHierarchyRelative(final String target, final Integer maxLevel) {
    this.target = Objects.requireNonNull(target, "target");

    final boolean hasInvalidMaxLevel = maxLevel != null && maxLevel < MIN_MAX_LEVEL;
    if (hasInvalidMaxLevel) {
      throw new IllegalArgumentException(
          "maxLevel must be at least " + MIN_MAX_LEVEL + ": " + maxLevel);
    }

    this.maxLevel = maxLevel;
  }

  /**
   * Returns the target concepts' name or code.
   *
   * @return the target name or code
   */
  public String getTarget() {
    return target;
  }

  /**
   * Sets the target concepts' name or code.
   *
   * @param target the target name or code
   */
  public void setTarget(final String target) {
    this.target = target;
  }

  /**
   * Returns the maximum distance from the targets.
   *
   * @return the max level, or null for any distance
   */
  public Integer getMaxLevel() {
    return maxLevel;
  }

  /**
   * Sets the maximum distance from the targets.
   *
   * @param maxLevel the max level, or null for any distance
   */
  public void setMaxLevel(final Integer maxLevel) {
    this.maxLevel = maxLevel;
  }

  /**
   * Returns the condition matching the targets' relatives in one terminology, not the targets
   * themselves (unless one target is a relative of another).
   *
   * @param operations the OpenSearch operations, for any lookups the subclass needs
   * @param terminology the terminology
   * @param targetCodes the target codes found in that terminology
   * @return the condition
   */
  protected abstract QueryBuilder fetchRelativesQuery(
      OpenSearchOperations operations, Terminology terminology, Set<String> targetCodes);

  /**
   * Returns the condition matching the targets' relatives, after looking up the target codes in
   * each terminology ({@link #fetchTargetCodes}). For the cases the concept's own fields can't
   * answer yet.
   *
   * @param operations the OpenSearch operations
   * @param terminologies the terminologies being searched
   * @return the condition
   */
  protected final QueryBuilder fetchRelativesOfTargetCodesQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    final List<QueryBuilder> terminologyQueries = new ArrayList<>();
    for (final Terminology terminology : terminologies) {
      final Set<String> targetCodes = fetchTargetCodes(operations, terminology);
      final boolean hasTargets = !targetCodes.isEmpty();
      if (hasTargets) {
        terminologyQueries.add(fetchTerminologyQuery(operations, terminology, targetCodes));
      }
    }

    final boolean hasTerminologyQueries = !terminologyQueries.isEmpty();
    return hasTerminologyQueries ? getAnyOfQuery(terminologyQueries) : getMatchNothingQuery();
  }

  /**
   * Returns the condition matching the targets themselves, for the "or is" subclasses: concepts
   * whose own name or code matches.
   *
   * @param operations the OpenSearch operations
   * @param terminologies the terminologies being searched
   * @return the condition
   */
  protected final QueryBuilder getTargetsQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    return new NameOrCodeMatch(NAME_FIELD, CODE_FIELD, Set.of(target))
        .fetchOpenSearchQuery(operations, terminologies);
  }

  /**
   * Returns the condition matching concepts whose code is in the set, or nothing for an empty set.
   * A concept's document id is its code.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology the codes are from
   * @param codes the codes
   * @return the condition
   */
  protected static QueryBuilder getCodeInQuery(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Set<String> codes) {
    final boolean hasCodes = !codes.isEmpty();
    if (!hasCodes) {
      return getMatchNothingQuery();
    }

    return new MatchAnyOf(ID_FIELD, codes).fetchOpenSearchQuery(operations, List.of(terminology));
  }

  /**
   * Looks up the code of every target concept in one terminology, a page at a time.
   *
   * <p><b>Why the matches are returned without their documents.</b> Only which codes matched is
   * needed. Concept documents can be very large (C3262's is about 1.4 MB, with its paths and
   * descendants), and OpenSearch loads and decompresses a whole document even to return a few of
   * its fields, or only its {@code _id}. Filtering {@code _source} to the code took about 40
   * seconds for the 15,233 concepts below C3262 in NCIt. So this search reads no stored fields at
   * all ({@link #NO_STORED_FIELDS}), and the code is the hit's sort value, read from the code's doc
   * values rather than the document. That takes under a second for the same concepts.
   *
   * <p>TODO: Remove once ancestors and descendants are indexed as nested {code, name, level}
   * entries.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology to look in
   * @return the target codes
   */
  protected final Set<String> fetchTargetCodes(
      final OpenSearchOperations operations, final Terminology terminology) {
    final QueryBuilder targetsQuery = getTargetsQuery(operations, List.of(terminology));

    final Set<String> targetCodes = new HashSet<>();

    // List<Object> is the shape of the page cursor that is relevant to OpenSearch
    // what is actually in it is largely irrelevant to us beyond the fact that OpenSearch
    // can use it as a pageCursor
    List<Object> pageCursor = null;

    boolean hasMorePages = true;
    while (hasMorePages) {
      final SearchHits<Concept> page =
          operations.search(
              getTargetCodePageQuery(targetsQuery, pageCursor),
              Concept.class,
              IndexCoordinates.of(terminology.getIndexName()));

      addPageCodes(targetCodes, page);

      // should continue leafing through pages and recording their codes?
      hasMorePages = page.getSearchHits().size() == CONCEPTS_PER_PAGE;
      pageCursor = hasMorePages ? getNextPageCursor(page) : null;
    }

    return targetCodes;
  }

  /**
   * Returns the condition for one terminology: in that terminology's index, and a relative of its
   * targets.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology
   * @param targetCodes the target codes found in that terminology
   * @return the condition
   */
  private QueryBuilder fetchTerminologyQuery(
      final OpenSearchOperations operations,
      final Terminology terminology,
      final Set<String> targetCodes) {
    final QueryBuilder relativesQuery = fetchRelativesQuery(operations, terminology, targetCodes);

    return getAllOfQuery(getInIndexQuery(operations, terminology), relativesQuery);
  }

  /**
   * Returns the condition matching documents in one terminology's index.
   *
   * @param operations the OpenSearch operations
   * @param terminology the terminology
   * @return the condition
   */
  private static QueryBuilder getInIndexQuery(
      final OpenSearchOperations operations, final Terminology terminology) {
    return new MatchAnyOf(INDEX_FIELD, Set.of(terminology.getIndexName()))
        .fetchOpenSearchQuery(operations, List.of(terminology));
  }

  /**
   * Adds each hit's code, its sort value.
   *
   * @param targetCodes the codes found so far, added to
   * @param page the page
   */
  private static void addPageCodes(final Set<String> targetCodes, final SearchHits<Concept> page) {
    for (final SearchHit<Concept> hit : page.getSearchHits()) {
      targetCodes.add((String) hit.getSortValues().get(CODE_SORT_VALUE_POSITION));
    }
  }

  /**
   * Builds the search for one page of target codes, ordered by code so pages don't overlap. It
   * returns no stored fields, only each hit's sort values (see {@link #fetchTargetCodes}).
   *
   * @param targetsQuery the query matching the targets
   * @param pageCursor where the page starts: the sort values of the previous page's last hit, or
   *     null for the first page
   * @return the search
   */
  private static NativeSearchQuery getTargetCodePageQuery(
      final QueryBuilder targetsQuery, final List<Object> pageCursor) {
    final NativeSearchQuery search =
        new NativeSearchQueryBuilder()
            .withQuery(targetsQuery)
            .withSorts(SortBuilders.fieldSort(CODE_FIELD).order(SortOrder.ASC))
            .withStoredFields(NO_STORED_FIELDS)
            .withPageable(PageRequest.of(FIRST_PAGE, CONCEPTS_PER_PAGE))
            .build();
    search.setSearchAfter(pageCursor);
    return search;
  }

  /**
   * Returns the cursor that starts the next page: the sort values of a page's last hit.
   *
   * @param page a non-empty page
   * @return the cursor
   */
  private static List<Object> getNextPageCursor(final SearchHits<Concept> page) {
    final List<SearchHit<Concept>> hits = page.getSearchHits();
    return hits.get(hits.size() - 1).getSortValues();
  }
}
