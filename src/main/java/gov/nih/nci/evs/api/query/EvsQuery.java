package gov.nih.nci.evs.api.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;

/**
 * A node in an EVS search query tree. The term searches ({@link Exact}, {@link StartsWith}, {@link
 * Contains}, {@link Fuzzy}) search names, codes, and synonyms like the legacy concept search; the
 * logical queries ({@link And}, {@link Or}, {@link Not}, {@link Boost}) combine other {@link
 * EvsQuery} nodes. Every other node holds only names, codes, and values, never another query.
 *
 * <p>Run a tree with {@link #fetchConcepts}. EXPERIMENTAL: nodes may also contribute sorts (see
 * {@link #getSorts()}).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = And.class, name = "And"),
  @JsonSubTypes.Type(value = Or.class, name = "Or"),
  @JsonSubTypes.Type(value = Not.class, name = "Not"),
  @JsonSubTypes.Type(value = Boost.class, name = "Boost"),
  @JsonSubTypes.Type(value = HasProperty.class, name = "HasProperty"),
  @JsonSubTypes.Type(value = HasSynonym.class, name = "HasSynonym"),
  @JsonSubTypes.Type(value = InSubset.class, name = "InSubset"),
  @JsonSubTypes.Type(value = Typeahead.class, name = "Typeahead"),
  @JsonSubTypes.Type(value = Exact.class, name = "Exact"),
  @JsonSubTypes.Type(value = StartsWith.class, name = "StartsWith"),
  @JsonSubTypes.Type(value = Contains.class, name = "Contains"),
  @JsonSubTypes.Type(value = Fuzzy.class, name = "Fuzzy"),
  @JsonSubTypes.Type(value = HasAncestor.class, name = "HasAncestor"),
  @JsonSubTypes.Type(value = HasAncestorOrIs.class, name = "HasAncestorOrIs"),
  @JsonSubTypes.Type(value = HasDescendant.class, name = "HasDescendant"),
  @JsonSubTypes.Type(value = HasDescendantOrIs.class, name = "HasDescendantOrIs"),
  @JsonSubTypes.Type(value = HasRole.class, name = "HasRole"),
  @JsonSubTypes.Type(value = HasInverseRole.class, name = "HasInverseRole"),
  @JsonSubTypes.Type(value = HasAssociation.class, name = "HasAssociation"),
  @JsonSubTypes.Type(value = HasInverseAssociation.class, name = "HasInverseAssociation")
})
@JsonInclude(Include.NON_EMPTY)
public abstract class EvsQuery {

  /**
   * Returns the OpenSearch query for this node. A hierarchy node may look up its target concepts'
   * codes first (see {@link HasHierarchyRelative#fetchTargetCodes}), which is why this may go to
   * OpenSearch.
   *
   * <p>Most nodes ignore {@code operations}, and only pass {@code terminologies} on to their
   * children. Both are parameters, rather than something nodes look up or hold themselves:
   *
   * <ul>
   *   <li>A static lookup (e.g. {@code StaticContextAccessor.getBean}) hides which nodes search,
   *       and mocking it changes global state shared by every test, or needs the whole Spring
   *       context.
   *   <li>Injecting it at construction (e.g. through a factory bean) means every tree containing a
   *       search needs {@code operations} just to be built. Taking it here keeps trees plain data
   *       that any code can build.
   *   <li>The same tree can then run against any terminologies, and a node can run a child against
   *       different ones.
   * </ul>
   *
   * <p>For example, a unit test can check a hierarchy node's query without OpenSearch or Spring:
   *
   * <pre>{@code
   * final OpenSearchOperations operations = mock(OpenSearchOperations.class);
   * // Stub search(...) to return a page holding the target code
   * when(operations.search(any(), eq(Concept.class), any())).thenReturn(skinAndLungHits);
   *
   * final HasAncestor query = new HasAncestor("C12470");
   * final QueryBuilder builder = query.fetchOpenSearchQuery(operations, terminologies);
   * // builder now matches concepts below Skin
   * }</pre>
   *
   * @param operations the OpenSearch operations, for any searches the node needs to run first
   * @param terminologies the terminologies being searched
   * @return the query builder
   */
  public abstract QueryBuilder fetchOpenSearchQuery(
      OpenSearchOperations operations, List<Terminology> terminologies);

  /**
   * Runs this query and returns one page of the concepts it matches. Sorts come from {@link
   * #getSorts()}, with duplicates dropped, followed by terminology and code so results are stable
   * between requests. When the include asks for highlights, each hit carries OpenSearch's
   * highlights of the fields the query matched.
   *
   * @param operations the OpenSearch operations
   * @param terminologies the terminologies to search
   * @param page the page to return
   * @param include the fields to return for each concept, and whether to highlight matches
   * @return the page of matching concepts, with the total match count
   * @throws IllegalArgumentException if a terminology is missing its terminology or version
   */
  public final SearchHits<Concept> fetchConcepts(
      final OpenSearchOperations operations,
      final List<Terminology> terminologies,
      final Pageable page,
      final IncludeParam include) {
    checkTerminologiesAreSearchable(terminologies);

    final NativeSearchQueryBuilder search =
        new NativeSearchQueryBuilder()
            .withQuery(fetchOpenSearchQuery(operations, terminologies))
            .withSorts(getSortsWithTieBreak())
            .withSourceFilter(
                new FetchSourceFilter(
                    true, include.getIncludedFields(), include.getExcludedFields()))
            .withPageable(page);
    final boolean shouldHighlightMatches = include.isHighlights();
    if (shouldHighlightMatches) {
      search.withHighlightFields(getHighlightFields().toArray(HighlightBuilder.Field[]::new));
    }
    return operations.search(search.build(), Concept.class, getIndexCoordinates(terminologies));
  }

  /**
   * Counts every concept this query matches, with no limit, unlike the total {@link #fetchConcepts}
   * returns, which stops at OpenSearch's default of 10,000.
   *
   * @param operations the OpenSearch operations
   * @param terminologies the terminologies to search
   * @return the number of matching concepts
   * @throws IllegalArgumentException if a terminology is missing its terminology or version
   */
  public final long fetchConceptCount(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    checkTerminologiesAreSearchable(terminologies);

    final NativeSearchQueryBuilder count =
        new NativeSearchQueryBuilder().withQuery(fetchOpenSearchQuery(operations, terminologies));
    return operations.count(count.build(), Concept.class, getIndexCoordinates(terminologies));
  }

  /**
   * Returns the concept indexes of the terminologies, which {@link #fetchConcepts} and {@link
   * #fetchConceptCount} search.
   *
   * @param terminologies the terminologies
   * @return the index coordinates
   */
  public static IndexCoordinates getIndexCoordinates(final List<Terminology> terminologies) {
    return IndexCoordinates.of(
        terminologies.stream().map(Terminology::getIndexName).toArray(String[]::new));
  }

  /**
   * EXPERIMENTAL: returns the sorts this node wants applied when it's the query {@link
   * #fetchConcepts} runs, in priority order. Most nodes only match, so the default is none.
   *
   * <p>Composite nodes must return their own sorts first, then each child's sorts in child order. A
   * nested node's sorts therefore only break ties left by the nodes above and before it. A node
   * that negates its children (e.g. {@code Not}) contributes none of their sorts.
   *
   * @return the sorts, highest priority first
   */
  protected List<SortBuilder<?>> getSorts() {
    return List.of();
  }

  /**
   * Returns the fields OpenSearch should highlight when {@link #fetchConcepts} is asked for
   * highlights. The default highlights every top-level field the query matched, as the main concept
   * search does.
   *
   * <p>Override it when the node matches subfields, e.g. a {@code search_as_you_type} subfield: the
   * {@code *} pattern doesn't reach multi-field subfields, so those matches would get no highlight.
   *
   * @return the fields to highlight
   */
  protected List<HighlightBuilder.Field> getHighlightFields() {
    return List.of(new HighlightBuilder.Field(EVERY_MATCHED_FIELD));
  }

  /**
   * Metadata field naming the index a hit came from. Each terminology has its own concept index, so
   * this orders by terminology. The terminology field itself is analyzed text and can't be sorted
   * on.
   */
  private static final String INDEX_FIELD = "_index";

  /** Highlights every top-level field the query matched, as the main concept search does. */
  private static final String EVERY_MATCHED_FIELD = "*";

  /** Field that orders concepts within a terminology. */
  private static final String CODE_FIELD = "code";

  /**
   * Fails if any terminology is missing its terminology or version. See {@link
   * #checkTerminologyIsSearchable}.
   *
   * @param terminologies the terminologies
   * @throws IllegalArgumentException if a terminology or version is missing
   */
  private static void checkTerminologiesAreSearchable(final List<Terminology> terminologies) {
    terminologies.forEach(EvsQuery::checkTerminologyIsSearchable);
  }

  /**
   * Fails if a terminology is missing its terminology or version, which name the terminology
   * version each hit comes from. Checked before each search, so a partly set up terminology fails
   * there, naming what's missing, rather than deep inside the search.
   *
   * <p>The index name isn't checked: {@link Terminology#getIndexName} is never null. Without one
   * set, it builds and caches one from the terminology and version, so it's only read after they
   * are known to be there.
   *
   * @param terminology the terminology
   * @throws IllegalArgumentException if the terminology or version is missing
   */
  private static void checkTerminologyIsSearchable(final Terminology terminology) {
    Objects.requireNonNull(terminology, "terminology");
    checkTerminologyHasField(terminology, "terminology", terminology.getTerminology());
    checkTerminologyHasField(terminology, "version", terminology.getVersion());
  }

  /**
   * Fails if one of a terminology's fields is missing, naming the field and showing the terminology
   * and version it does have.
   *
   * @param terminology the terminology
   * @param fieldName the name of the field
   * @param fieldValue the field's value
   * @throws IllegalArgumentException if the value is missing
   */
  private static void checkTerminologyHasField(
      final Terminology terminology, final String fieldName, final String fieldValue) {
    final boolean hasFieldValue = fieldValue != null;
    if (!hasFieldValue) {
      throw new IllegalArgumentException(
          "Terminology has no "
              + fieldName
              + " (terminology = "
              + terminology.getTerminology()
              + ", version = "
              + terminology.getVersion()
              + ")");
    }
  }

  /**
   * Returns this node's sorts without duplicates, then the terminology and code tie-break.
   *
   * @return the sorts, highest priority first
   */
  private List<SortBuilder<?>> getSortsWithTieBreak() {
    final List<SortBuilder<?>> sorts = new ArrayList<>(getSorts());
    sorts.add(SortBuilders.fieldSort(INDEX_FIELD).order(SortOrder.ASC));
    sorts.add(SortBuilders.fieldSort(CODE_FIELD).order(SortOrder.ASC));
    return sorts.stream().distinct().collect(Collectors.toList());
  }
}
