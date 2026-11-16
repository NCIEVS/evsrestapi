package gov.nih.nci.evs.api.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.util.List;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;

/**
 * A node in an EVS search query tree. The term searches ({@link Exact}, {@link StartsWith}, {@link
 * Contains}, {@link Fuzzy}) search names, codes, and synonyms like the legacy concept search; the
 * logical queries ({@link And}, {@link Or}, {@link Not}, {@link Boost}) combine other {@link
 * EvsQuery} nodes. Every other node holds only names, codes, and values, never another query.
 *
 * <p>Run a tree with {@link #fetchConcepts}. Nodes only match: none adds sorts, so matches come
 * back in OpenSearch's default order, highest score first. To order them another way, add sorts to
 * a search built around {@link #fetchOpenSearchQuery}.
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
   * Runs this query and returns one page of the concepts it matches, in OpenSearch's default order,
   * highest score first. When the include asks for highlights, each hit carries OpenSearch's
   * highlights of every top-level field the query matched, as the main concept search does.
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
    TerminologyUtils.checkTerminologiesAreSearchable(terminologies);

    final NativeSearchQueryBuilder search =
        new NativeSearchQueryBuilder()
            .withQuery(fetchOpenSearchQuery(operations, terminologies))
            .withSourceFilter(
                new FetchSourceFilter(
                    true, include.getIncludedFields(), include.getExcludedFields()))
            .withPageable(page);
    final boolean shouldHighlightMatches = include.isHighlights();
    if (shouldHighlightMatches) {
      search.withHighlightFields(new HighlightBuilder.Field(EVERY_MATCHED_FIELD));
    }
    return operations.search(
        search.build(), Concept.class, TerminologyUtils.getIndexCoordinates(terminologies));
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
    TerminologyUtils.checkTerminologiesAreSearchable(terminologies);

    final NativeSearchQueryBuilder count =
        new NativeSearchQueryBuilder().withQuery(fetchOpenSearchQuery(operations, terminologies));
    return operations.count(
        count.build(), Concept.class, TerminologyUtils.getIndexCoordinates(terminologies));
  }

  /** Highlights every top-level field the query matched, as the main concept search does. */
  private static final String EVERY_MATCHED_FIELD = "*";
}
