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
 * Unit tests for {@link HasHierarchyRelative#fetchTargetCodes}, which looks up the code of every
 * target concept in one terminology, a page at a time.
 *
 * <p>No OpenSearch or Spring: a mocked {@link OpenSearchOperations} returns pages of made-up hits.
 * That makes it cheap to return more than one page, which would otherwise take tens of thousands of
 * indexed documents.
 */
public class HasHierarchyRelativeTargetCodesTest {

  /** Index name the mocked pages claim their hits came from. */
  private static final String INDEX_NAME = "concept_ncit_2512e";

  /** Terminology of the made-up concepts. */
  private static final String TERMINOLOGY = "ncit";

  /** Version of the made-up concepts. */
  private static final String VERSION = "25.12e";

  /** Hits on a page that isn't full, so it's the last page. */
  private static final int PARTIAL_PAGE_SIZE = 3;

  /** The number in the first page's first code. */
  private static final int FIRST_CODE_NUMBER = 0;

  /** The terminology searched. */
  private Terminology terminology;

  /** Everything below Skin. The mock ignores what the targets query matches. */
  private HasHierarchyRelative descendantsOfSkin;

  /** Mocked OpenSearch operations, stubbed per test. */
  private OpenSearchOperations operations;

  /** Builds the query and a fresh mock. */
  @BeforeEach
  public void setUp() {
    terminology = QueryTestTerminologies.getTerminology(TERMINOLOGY, VERSION, INDEX_NAME);
    descendantsOfSkin = new HasAncestor("C12470");
    operations = mock(OpenSearchOperations.class);
  }

  /** One page that isn't full is the only page: one search, all of its codes. */
  @Test
  public void testPartialPageIsTheLastPage() {
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(getPage(FIRST_CODE_NUMBER, PARTIAL_PAGE_SIZE));

    final Set<String> targetCodes = descendantsOfSkin.fetchTargetCodes(operations, terminology);

    assertEquals(PARTIAL_PAGE_SIZE, targetCodes.size());
    captureSearches(1);
  }

  /**
   * No stored fields are fetched, not even {@code _source}: concept documents can be very large,
   * and the sort value identifies each hit.
   */
  @Test
  public void testFetchesNoStoredFields() {
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(getPage(FIRST_CODE_NUMBER, PARTIAL_PAGE_SIZE));

    descendantsOfSkin.fetchTargetCodes(operations, terminology);

    final NativeSearchQuery search = (NativeSearchQuery) captureSearches(1).get(0);
    assertEquals(List.of(HasHierarchyRelative.NO_STORED_FIELDS), search.getStoredFields());
    assertNull(search.getSourceFilter());
  }

  /**
   * Each hit's code is its first sort value. The hit's content isn't read: with no stored fields,
   * it's empty.
   */
  @Test
  public void testCodeComesFromFirstSortValue() {
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(getPage(List.of(getHit("C1"), getHit("C2"))));

    final Set<String> targetCodes = descendantsOfSkin.fetchTargetCodes(operations, terminology);

    assertEquals(Set.of("C1", "C2"), targetCodes);
  }

  /**
   * A full page means there may be more, so the next page is fetched starting after the last hit's
   * sort values. Codes from every page are returned.
   */
  @Test
  public void testFullPageFetchesTheNextPageAfterItsLastHit() {
    final SearchHits<Concept> fullPage =
        getPage(FIRST_CODE_NUMBER, HasHierarchyRelative.CONCEPTS_PER_PAGE);
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(fullPage, getPage(HasHierarchyRelative.CONCEPTS_PER_PAGE, PARTIAL_PAGE_SIZE));

    final Set<String> targetCodes = descendantsOfSkin.fetchTargetCodes(operations, terminology);

    assertEquals(HasHierarchyRelative.CONCEPTS_PER_PAGE + PARTIAL_PAGE_SIZE, targetCodes.size());
    final List<Query> searches = captureSearches(2);
    assertNull(searches.get(0).getSearchAfter());
    final List<SearchHit<Concept>> firstPageHits = fullPage.getSearchHits();
    final List<Object> lastHitSortValues =
        firstPageHits.get(firstPageHits.size() - 1).getSortValues();
    assertEquals(lastHitSortValues, searches.get(1).getSearchAfter());
  }

  /**
   * A last page of exactly {@link HasHierarchyRelative#CONCEPTS_PER_PAGE} hits looks like there may
   * be more, so one more search runs and comes back empty.
   */
  @Test
  public void testExactlyFullLastPageEndsOnAnEmptyPage() {
    final int noHits = 0;
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(
            getPage(FIRST_CODE_NUMBER, HasHierarchyRelative.CONCEPTS_PER_PAGE),
            getPage(HasHierarchyRelative.CONCEPTS_PER_PAGE, noHits));

    final Set<String> targetCodes = descendantsOfSkin.fetchTargetCodes(operations, terminology);

    assertEquals(HasHierarchyRelative.CONCEPTS_PER_PAGE, targetCodes.size());
    captureSearches(2);
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
   * Builds a page of made-up hits with consecutive codes.
   *
   * @param firstCodeNumber the number in the first hit's code
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
   * Builds one hit as a search with no stored fields returns it, sorted by code: an empty concept,
   * no id, and the code only in its sort values.
   *
   * @param code the concept code
   * @return the hit
   */
  private static SearchHit<Concept> getHit(final String code) {
    final float score = 0f;
    final Object[] sortValues = {code};
    return new SearchHit<>(
        INDEX_NAME,
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
