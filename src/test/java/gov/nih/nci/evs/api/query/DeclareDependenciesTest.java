package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * Unit tests for {@link EvsQuery#declareDependencies}, which runs a query about another concept
 * (e.g. a role target) and returns every match, a page at a time.
 *
 * <p>No OpenSearch or Spring: a mocked {@link OpenSearchOperations} returns pages of made-up
 * concepts. That makes it cheap to return more than one page, which would otherwise take tens of
 * thousands of indexed documents.
 */
@DemonstratesFieldMatch
public class DeclareDependenciesTest {

  /** Index name the mocked pages claim their hits came from. */
  private static final String INDEX_NAME = "concept_test";

  /** Terminology of the made-up concepts. */
  private static final String TERMINOLOGY = "ncit";

  /** Version of the made-up concepts. */
  private static final String VERSION = "25.12e";

  /** Hits on a page that isn't full, so it's the last page. */
  private static final int PARTIAL_PAGE_SIZE = 3;

  /** The current version of the made-up terminology, for queries over two versions. */
  private static final String CURRENT_VERSION = "25.12e";

  /** The previous version of the made-up terminology, for queries over two versions. */
  private static final String PREVIOUS_VERSION = "25.11d";

  /** The terminology searched. */
  private List<Terminology> terminologies;

  /**
   * Concepts with Skin's code, whose matching concepts are fetched. The mock ignores what it
   * matches.
   */
  private EvsQuery conceptsWithSkinCode;

  /** Mocked OpenSearch operations, stubbed per test. */
  private OpenSearchOperations operations;

  /** Builds the query and a fresh mock. */
  @BeforeEach
  public void setUp() {
    terminologies =
        List.of(QueryTestTerminologies.getTerminology(TERMINOLOGY, VERSION, INDEX_NAME));
    conceptsWithSkinCode = new FieldMatch(terminologies, "code", new Exact("C12470"));
    operations = mock(OpenSearchOperations.class);
  }

  /** One page that isn't full is the only page: one search, all of its concepts. */
  @Test
  public void testPartialPageIsTheLastPage() {
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(getPage(0, PARTIAL_PAGE_SIZE));

    final int codeCount = fetchDependencyCodeCount();

    assertEquals(PARTIAL_PAGE_SIZE, codeCount);
    captureSearches(1);
  }

  /**
   * No stored fields are fetched, not even {@code _source}: concept documents can be very large,
   * and the sort values and index identify each hit.
   */
  @Test
  public void testFetchesNoStoredFields() {
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(getPage(0, PARTIAL_PAGE_SIZE));

    fetchDependencyCodeCount();

    final NativeSearchQuery search = (NativeSearchQuery) captureSearches(1).get(0);
    assertEquals(List.of(EvsQuery.NO_STORED_FIELDS), search.getStoredFields());
    assertNull(search.getSourceFilter());
  }

  /**
   * Each hit's code is its first sort value. The hit's content isn't read: with no stored fields,
   * it's empty.
   */
  @Test
  public void testCodeComesFromFirstSortValue() {
    final ConceptKeys keys = fetchTwoVersionKeys();

    final Set<String> codes =
        keys.getCodesByTerminologyVersion().values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    assertEquals(Set.of("C1", "C2"), codes);
  }

  /** Each hit's terminology version is that of the index it came from. */
  @Test
  public void testTerminologyVersionComesFromHitIndex() {
    final ConceptKeys keys = fetchTwoVersionKeys();

    assertEquals(
        Map.of(
            new ConceptKeys.TerminologyVersion(TERMINOLOGY, CURRENT_VERSION), Set.of("C1", "C2"),
            new ConceptKeys.TerminologyVersion(TERMINOLOGY, PREVIOUS_VERSION), Set.of("C1")),
        keys.getCodesByTerminologyVersion());
  }

  /**
   * Runs a dependency query over two versions of the terminology, whose search returns empty hits:
   * C1 and C2 from the current version's index, and C1 from the previous version's.
   *
   * @return the keys the dependency found
   */
  private ConceptKeys fetchTwoVersionKeys() {
    final Terminology current =
        QueryTestTerminologies.getTerminology(TERMINOLOGY, CURRENT_VERSION, "concept_ncit_2512e");
    final Terminology previous =
        QueryTestTerminologies.getTerminology(TERMINOLOGY, PREVIOUS_VERSION, "concept_ncit_2511d");
    final EvsQuery conceptsWithSkinCodeInTwoVersions =
        new FieldMatch(List.of(current, previous), "code", new Exact("C12470"));
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(
            getPage(
                List.of(
                    getEmptyHit("C1", current.getIndexName()),
                    getEmptyHit("C1", previous.getIndexName()),
                    getEmptyHit("C2", current.getIndexName()))));

    return EvsQuery.declareDependencies(operations, conceptsWithSkinCodeInTwoVersions).get(0);
  }

  /**
   * A full page means there may be more, so the next page is fetched starting after the last hit's
   * sort values. Concepts from every page are returned.
   */
  @Test
  public void testFullPageFetchesTheNextPageAfterItsLastHit() {
    final SearchHits<Concept> fullPage = getPage(0, EvsQuery.CONCEPTS_PER_PAGE);
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(fullPage, getPage(EvsQuery.CONCEPTS_PER_PAGE, PARTIAL_PAGE_SIZE));

    final int codeCount = fetchDependencyCodeCount();

    assertEquals(EvsQuery.CONCEPTS_PER_PAGE + PARTIAL_PAGE_SIZE, codeCount);
    final List<Query> searches = captureSearches(2);
    assertNull(searches.get(0).getSearchAfter());
    final List<SearchHit<Concept>> firstPageHits = fullPage.getSearchHits();
    final List<Object> lastHitSortValues =
        firstPageHits.get(firstPageHits.size() - 1).getSortValues();
    assertEquals(lastHitSortValues, searches.get(1).getSearchAfter());
  }

  /**
   * A last page of exactly {@link EvsQuery#CONCEPTS_PER_PAGE} hits looks like there may be more, so
   * one more search runs and comes back empty.
   */
  @Test
  public void testExactlyFullLastPageEndsOnAnEmptyPage() {
    final int noHits = 0;
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(
            getPage(0, EvsQuery.CONCEPTS_PER_PAGE), getPage(EvsQuery.CONCEPTS_PER_PAGE, noHits));

    final int codeCount = fetchDependencyCodeCount();

    assertEquals(EvsQuery.CONCEPTS_PER_PAGE, codeCount);
    captureSearches(2);
  }

  /**
   * Runs {@link #conceptsWithSkinCode} as a dependency and counts the codes it found.
   *
   * @return the number of codes found, across terminology versions
   */
  private int fetchDependencyCodeCount() {
    final ConceptKeys keys = EvsQuery.declareDependencies(operations, conceptsWithSkinCode).get(0);
    return keys.getCodesByTerminologyVersion().values().stream().mapToInt(Set::size).sum();
  }

  /**
   * Verifies the number of searches run and returns their queries, in order.
   *
   * @param expectedSearches how many searches should have run
   * @return the queries searched
   */
  private List<Query> captureSearches(final int expectedSearches) {
    final ArgumentCaptor<Query> searches = ArgumentCaptor.forClass(Query.class);
    verify(operations, times(expectedSearches))
        .search(searches.capture(), eq(Concept.class), any(IndexCoordinates.class));
    return searches.getAllValues();
  }

  /**
   * Builds a page of made-up concepts with consecutive codes, each hit sorted by code then index
   * like the real concept-key page search.
   *
   * @param firstCodeNumber the number in the first concept's code
   * @param hitCount how many hits the page holds
   * @return the page
   */
  private static SearchHits<Concept> getPage(final int firstCodeNumber, final int hitCount) {
    final List<SearchHit<Concept>> hits = new ArrayList<>();
    for (int codeNumber = firstCodeNumber; codeNumber < firstCodeNumber + hitCount; codeNumber++) {
      hits.add(getHit("C" + codeNumber));
    }
    return getPage(hits);
  }

  /**
   * Builds a page holding the given hits.
   *
   * @param hits the hits
   * @return the page
   */
  private static SearchHits<Concept> getPage(final List<SearchHit<Concept>> hits) {
    final float maxScore = 0f;
    return new SearchHitsImpl<>(
        hits.size(),
        TotalHitsRelation.EQUAL_TO,
        maxScore,
        Duration.ZERO,
        null,
        null,
        hits,
        null,
        null,
        null);
  }

  /**
   * Builds one hit for a concept with the given code.
   *
   * @param code the concept code
   * @return the hit
   */
  private static SearchHit<Concept> getHit(final String code) {
    final Concept concept = new Concept();
    concept.setCode(code);
    concept.setTerminology(TERMINOLOGY);
    concept.setVersion(VERSION);
    final float score = 0f;
    final Object[] sortValues = {code, INDEX_NAME};
    return new SearchHit<>(
        INDEX_NAME,
        code,
        null,
        score,
        sortValues,
        Map.of(),
        Map.of(),
        null,
        null,
        List.of(),
        concept);
  }

  /**
   * Builds one hit as a search with no stored fields returns it: an empty concept, no id, and the
   * code and index only in its sort values.
   *
   * @param code the concept code
   * @param indexName the index the hit came from
   * @return the hit
   */
  private static SearchHit<Concept> getEmptyHit(final String code, final String indexName) {
    final float score = 0f;
    final Object[] sortValues = {code, indexName};
    return new SearchHit<>(
        indexName,
        null,
        null,
        score,
        sortValues,
        Map.of(),
        Map.of(),
        null,
        null,
        List.of(),
        new Concept());
  }
}
